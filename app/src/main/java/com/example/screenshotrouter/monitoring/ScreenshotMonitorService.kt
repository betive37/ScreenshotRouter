package com.example.screenshotrouter.monitoring

import android.annotation.TargetApi
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

class ScreenshotMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val shutdownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var logRepository: LogRepository
    private lateinit var notificationController: MonitorNotificationController
    private lateinit var overlayController: ScreenshotOverlayController
    private lateinit var screenshotRouter: ScreenshotRouter
    private lateinit var detector: ScreenshotDetector

    private var running = false
    private var stopLogged = false
    private var latestEvent: ScreenshotEvent? = null
    private val activeRouteEvents = linkedMapOf<String, ScreenshotEvent>()

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
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoringAndSelf(getString(R.string.log_monitoring_stopped_by_user))
            ACTION_ROUTE_LEFT -> routeFromIntent(intent, SwipeDirection.Left)
            ACTION_ROUTE_RIGHT -> routeFromIntent(intent, SwipeDirection.Right)
            ACTION_DISMISS -> dismissRouteIntent(intent)
            null -> {
                scope.launch { logRepository.append(getString(R.string.log_monitoring_ignored_null_start)) }
                stopSelf(startId)
            }
            else -> {
                scope.launch { logRepository.append(getString(R.string.log_monitoring_ignored_unknown_action)) }
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @TargetApi(35)
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopMonitoringAndSelf(getString(R.string.log_monitoring_timeout))
    }

    override fun onDestroy() {
        val wasRunning = running
        if (wasRunning) {
            running = false
            if (::detector.isInitialized) detector.stop()
            if (::overlayController.isInitialized) overlayController.dismiss()
            cancelActiveRouteNotifications()
        }
        if (::settingsRepository.isInitialized && ::logRepository.isInitialized) {
            shutdownScope.launch {
                settingsRepository.setMonitoringActive(false)
                if (wasRunning && !stopLogged) {
                    logRepository.append(getString(R.string.log_monitoring_stopped))
                }
            }
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (running) return
        val rejection = monitoringStartRejection()
        if (rejection != null) {
            rejectStart(rejection)
            return
        }

        notificationController.ensureChannels()
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        val foregroundStarted = runCatching {
            ServiceCompat.startForeground(
                this,
                NotificationIds.MONITOR_NOTIFICATION_ID,
                notificationController.buildMonitoringNotification(),
                foregroundType
            )
        }.onFailure { error ->
            rejectStart(getString(R.string.log_start_failed_foreground, error.message ?: error::class.java.simpleName))
        }.isSuccess
        if (!foregroundStarted) return

        running = true
        stopLogged = false
        detector.start()
        scope.launch {
            settingsRepository.setMonitoringActive(true)
            logRepository.append(getString(R.string.log_monitoring_started))
            if (!OverlayPermission.canDrawOverlays(applicationContext)) {
                logRepository.append(getString(R.string.log_overlay_missing_fallback))
            }
        }
    }

    private fun monitoringStartRejection(): String? {
        if (!PermissionChecks.hasFullImageReadAccess(applicationContext)) {
            return if (PermissionChecks.hasPartialVisualAccess(applicationContext)) {
                getString(R.string.log_start_blocked_partial_media)
            } else {
                getString(R.string.log_start_blocked_missing_full_media)
            }
        }
        if (!canPresentRouteUi()) {
            return getString(R.string.log_start_blocked_no_route_ui)
        }
        return null
    }

    private fun canPresentRouteUi(): Boolean =
        OverlayPermission.canDrawOverlays(applicationContext) || PermissionChecks.hasNotificationPermission(applicationContext)

    private fun rejectStart(message: String) {
        shutdownScope.launch {
            settingsRepository.setMonitoringActive(false)
            logRepository.append(message)
        }
        stopSelf()
    }

    private fun stopMonitoringAndSelf(stopMessage: String) {
        val wasRunning = running
        running = false
        if (::detector.isInitialized) detector.stop()
        if (::overlayController.isInitialized) overlayController.dismiss()
        cancelActiveRouteNotifications()
        latestEvent = null
        stopLogged = true
        scope.launch {
            settingsRepository.setMonitoringActive(false)
            if (wasRunning) logRepository.append(stopMessage)
        }
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun routeFromIntent(intent: Intent, direction: SwipeDirection) {
        val event = ScreenshotEventExtras.run { intent.getScreenshotEvent() } ?: latestEvent
        if (!running) {
            notificationController.cancelRouteNotification(event)
            scope.launch { logRepository.append(getString(R.string.log_route_action_after_stop)) }
            stopSelf()
            return
        }
        if (event == null) {
            scope.launch { logRepository.append(getString(R.string.log_route_action_no_event)) }
            return
        }
        activeRouteEvents.remove(event.eventKey())
        notificationController.cancelRouteNotification(event)
        routeScreenshot(event, direction)
    }

    private fun dismissRouteIntent(intent: Intent) {
        val event = ScreenshotEventExtras.run { intent.getScreenshotEvent() }
        notificationController.cancelRouteNotification(event)
        event?.let { activeRouteEvents.remove(it.eventKey()) }
        if (!running) {
            scope.launch { logRepository.append(getString(R.string.log_route_action_after_stop)) }
            stopSelf()
            return
        }
        scope.launch { logRepository.append(getString(R.string.log_routing_notification_dismissed)) }
    }

    private suspend fun handleDetectedScreenshot(event: ScreenshotEvent) {
        if (!running) return
        latestEvent = event
        val overlayShown = OverlayPermission.canDrawOverlays(applicationContext) &&
            overlayController.show(event) { direction -> routeScreenshot(event, direction) }
        if (!overlayShown) {
            if (PermissionChecks.hasNotificationPermission(applicationContext)) {
                activeRouteEvents[event.eventKey()] = event
                notificationController.showRouteFallback(event)
            } else {
                logRepository.append(getString(R.string.log_route_ui_lost_stopping))
                stopMonitoringAndSelf(getString(R.string.log_route_ui_lost_stopping))
            }
        }
    }

    private fun routeScreenshot(event: ScreenshotEvent, direction: SwipeDirection) {
        if (!running) {
            scope.launch { logRepository.append(getString(R.string.log_route_action_after_stop)) }
            return
        }
        scope.launch {
            if (!running) return@launch
            val settings = settingsRepository.settings.first()
            val destination = DestinationResolver.findDestination(settings.destinations, direction)
            val result = if (destination == null) {
                RouteResult.Failed(getString(R.string.route_failure_no_destination, direction.name.lowercase()))
            } else {
                screenshotRouter.route(event, destination, settings.routeMode)
            }
            if (!running) return@launch
            if (result is RouteResult.DeleteConsentRequired && destination != null) {
                logRepository.append(RouteResultFormatter.format(applicationContext, result))
                if (PermissionChecks.hasNotificationPermission(applicationContext)) {
                    notificationController.showDeleteConsentRequired(event, destination.label)
                } else {
                    logRepository.append(
                        getString(R.string.log_delete_consent_notification_unavailable, destination.label)
                    )
                }
            } else {
                logRepository.append(RouteResultFormatter.format(applicationContext, result))
                notificationController.showRouteResult(result)
            }
        }
    }

    private fun cancelActiveRouteNotifications() {
        activeRouteEvents.values.forEach(notificationController::cancelRouteNotification)
        activeRouteEvents.clear()
    }

    private fun ScreenshotEvent.eventKey(): String = uri.toString()

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
