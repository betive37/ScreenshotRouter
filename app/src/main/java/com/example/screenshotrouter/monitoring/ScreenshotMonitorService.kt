package com.example.screenshotrouter.monitoring

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.RouteResult
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.model.SwipeDirection
import com.example.screenshotrouter.data.DataStoreLogRepository
import com.example.screenshotrouter.data.DataStoreSettingsRepository
import com.example.screenshotrouter.data.LogRepository
import com.example.screenshotrouter.data.SettingsRepository
import com.example.screenshotrouter.detection.ScreenshotDeduplicator
import com.example.screenshotrouter.detection.ScreenshotDetector
import com.example.screenshotrouter.overlay.OverlayPermission
import com.example.screenshotrouter.overlay.ScreenshotOverlayController
import com.example.screenshotrouter.permissions.PermissionChecks
import com.example.screenshotrouter.storage.DestinationResolver
import com.example.screenshotrouter.storage.ScreenshotRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ScreenshotMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var logRepository: LogRepository
    private lateinit var notificationController: MonitorNotificationController
    private lateinit var overlayController: ScreenshotOverlayController
    private lateinit var screenshotRouter: ScreenshotRouter
    private lateinit var detector: ScreenshotDetector

    private var running = false
    private var latestEvent: ScreenshotEvent? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = DataStoreSettingsRepository(applicationContext)
        logRepository = DataStoreLogRepository(applicationContext)
        notificationController = MonitorNotificationController(applicationContext)
        overlayController = ScreenshotOverlayController(applicationContext, scope)
        screenshotRouter = ScreenshotRouter(applicationContext)
        detector = ScreenshotDetector(
            context = applicationContext,
            scope = scope,
            deduplicator = ScreenshotDeduplicator(),
            onDetected = ::handleDetectedScreenshot,
            onStatus = { message -> logRepository.append(message) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMonitoringAndSelf()
            ACTION_ROUTE_LEFT -> routeFromIntent(intent, SwipeDirection.Left)
            ACTION_ROUTE_RIGHT -> routeFromIntent(intent, SwipeDirection.Right)
            ACTION_DISMISS -> {
                val event = ScreenshotEventExtras.run { intent.getScreenshotEvent() }
                notificationController.cancelRouteNotification(event)
                scope.launch { logRepository.append(getString(R.string.log_routing_notification_dismissed)) }
            }
            ACTION_START,
            null -> startMonitoring()
            else -> startMonitoring()
        }
        return if (running) START_STICKY else START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (running) {
            detector.stop()
            overlayController.dismiss()
        }
        runBlocking {
            settingsRepository.setMonitoringActive(false)
            logRepository.append(getString(R.string.log_monitoring_stopped))
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (running) return
        notificationController.ensureChannels()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            MonitorNotificationController.MONITOR_NOTIFICATION_ID,
            notificationController.buildMonitoringNotification(),
            foregroundType
        )
        running = true
        detector.start()
        scope.launch {
            settingsRepository.setMonitoringActive(true)
            logRepository.append(getString(R.string.log_monitoring_started))
            if (!PermissionChecks.hasFullImageReadAccess(applicationContext)) {
                val message = if (PermissionChecks.hasPartialVisualAccess(applicationContext)) {
                    getString(R.string.log_media_permission_partial)
                } else {
                    getString(R.string.log_media_permission_missing_detection)
                }
                logRepository.append(message)
            }
            if (!OverlayPermission.canDrawOverlays(applicationContext)) {
                logRepository.append(getString(R.string.log_overlay_missing_fallback))
            }
        }
    }

    private fun stopMonitoringAndSelf() {
        if (running) {
            running = false
            detector.stop()
            overlayController.dismiss()
        }
        scope.launch {
            settingsRepository.setMonitoringActive(false)
            logRepository.append(getString(R.string.log_monitoring_stopped_by_user))
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun routeFromIntent(intent: Intent, direction: SwipeDirection) {
        val event = ScreenshotEventExtras.run { intent.getScreenshotEvent() } ?: latestEvent
        if (event == null) {
            scope.launch { logRepository.append(getString(R.string.log_route_action_no_event)) }
            return
        }
        notificationController.cancelRouteNotification(event)
        routeScreenshot(event, direction)
    }

    private suspend fun handleDetectedScreenshot(event: ScreenshotEvent) {
        latestEvent = event
        val overlayShown = OverlayPermission.canDrawOverlays(applicationContext) &&
            overlayController.show(event) { direction -> routeScreenshot(event, direction) }
        if (!overlayShown) {
            notificationController.showRouteFallback(event)
        }
    }

    private fun routeScreenshot(event: ScreenshotEvent, direction: SwipeDirection) {
        scope.launch {
            val settings = settingsRepository.settings.first()
            val destination = DestinationResolver.findDestination(settings.destinations, direction)
            val result = if (destination == null) {
                RouteResult.Failed(getString(R.string.route_failure_no_destination, direction.name.lowercase()))
            } else {
                screenshotRouter.route(event, destination, settings.routeMode)
            }
            if (result is RouteResult.DeleteConsentRequired && destination != null) {
                logRepository.append(RouteResultFormatter.format(applicationContext, result))
                notificationController.showDeleteConsentRequired(event, destination.label)
                launchDeleteConsent(event, destination.label)
            } else {
                logRepository.append(RouteResultFormatter.format(applicationContext, result))
                notificationController.showRouteResult(result)
            }
        }
    }

    private fun launchDeleteConsent(event: ScreenshotEvent, destinationLabel: String) {
        val intent = DeleteConsentActivity.intent(applicationContext, event, destinationLabel)
        runCatching { startActivity(intent) }
            .onFailure {
                scope.launch {
                    logRepository.append(
                        getString(
                            R.string.log_delete_consent_activity_deferred,
                            it.message ?: it::class.java.simpleName
                        )
                    )
                }
            }
    }

    companion object {
        const val ACTION_START = "com.example.screenshotrouter.action.START"
        const val ACTION_STOP = "com.example.screenshotrouter.action.STOP"
        const val ACTION_ROUTE_LEFT = "com.example.screenshotrouter.action.ROUTE_LEFT"
        const val ACTION_ROUTE_RIGHT = "com.example.screenshotrouter.action.ROUTE_RIGHT"
        const val ACTION_DISMISS = "com.example.screenshotrouter.action.DISMISS"

        fun startIntent(context: Context): Intent = Intent(context, ScreenshotMonitorService::class.java).setAction(ACTION_START)
        fun stopIntent(context: Context): Intent = Intent(context, ScreenshotMonitorService::class.java).setAction(ACTION_STOP)
    }
}
