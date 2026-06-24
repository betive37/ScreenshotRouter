package com.example.screenshotrouter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.AppSettings
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.LogEntry
import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.SwipeDirection
import com.example.screenshotrouter.core.util.AppManagedPaths
import com.example.screenshotrouter.data.DataStoreLogRepository
import com.example.screenshotrouter.data.DataStoreSettingsRepository
import com.example.screenshotrouter.monitoring.ScreenshotMonitorService
import com.example.screenshotrouter.overlay.OverlayPermission
import com.example.screenshotrouter.permissions.PermissionChecks
import com.example.screenshotrouter.storage.SafPermissionVerifier
import com.example.screenshotrouter.ui.destinations.DestinationMappingScreen
import com.example.screenshotrouter.ui.log.LocalLogScreen
import com.example.screenshotrouter.ui.monitor.MonitorScreen
import com.example.screenshotrouter.ui.setup.SetupChecklistScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { DataStoreSettingsRepository(applicationContext) }
    private val logRepository by lazy { DataStoreLogRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val initialSettings = AppSettings(
                leftDestination = AppSettings.defaultDestination(SwipeDirection.Left).copy(
                    label = getString(R.string.default_destination_b)
                ),
                rightDestination = AppSettings.defaultDestination(SwipeDirection.Right).copy(
                    label = getString(R.string.default_destination_a)
                )
            )
            val settings by settingsRepository.settings.collectAsState(initial = initialSettings)
            val logs by logRepository.entries.collectAsState(initial = emptyList())
            ScreenshotRouterApp(
                settings = settings,
                logs = logs,
                saveDestination = { destination -> settingsRepository.saveDestination(destination) },
                setRouteMode = { mode -> settingsRepository.setRouteMode(mode) },
                appendLog = { message -> logRepository.append(message) },
                clearLog = { logRepository.clear() },
                startMonitoring = { startMonitoringService() },
                stopMonitoring = { stopMonitoringService() },
                takePersistableTreePermission = ::takePersistableTreePermission
            )
        }
    }

    private fun startMonitoringService() {
        ContextCompat.startForegroundService(this, ScreenshotMonitorService.startIntent(this))
    }

    private fun stopMonitoringService() {
        startService(ScreenshotMonitorService.stopIntent(this))
    }

    private fun takePersistableTreePermission(uri: Uri): Boolean {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            .isSuccess && SafPermissionVerifier.hasPersistedReadWritePermission(this, uri)
    }
}

private enum class MainTab(val label: String) {
    Setup("S"),
    Destinations("D"),
    Monitor("M"),
    Log("L")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenshotRouterApp(
    settings: AppSettings,
    logs: List<LogEntry>,
    saveDestination: suspend (Destination) -> Unit,
    setRouteMode: suspend (RouteMode) -> Unit,
    appendLog: suspend (String) -> Unit,
    clearLog: suspend () -> Unit,
    startMonitoring: () -> Unit,
    stopMonitoring: () -> Unit,
    takePersistableTreePermission: (Uri) -> Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(MainTab.Setup) }
    var permissionRefresh by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRefresh++
        scope.launch {
            appendLog(
                context.getString(
                    if (granted) R.string.log_notification_permission_granted else R.string.log_notification_permission_denied
                )
            )
        }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        permissionRefresh++
        val fullGranted = PermissionChecks.hasFullImageReadAccess(context)
        val partial = PermissionChecks.hasPartialVisualAccess(context)
        val message = when {
            fullGranted -> context.getString(R.string.log_media_permission_full)
            partial -> context.getString(R.string.log_media_permission_partial)
            else -> context.getString(R.string.log_media_permission_denied)
        }
        scope.launch { appendLog(message) }
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionRefresh++
        scope.launch {
            appendLog(
                context.getString(
                    if (OverlayPermission.canDrawOverlays(context)) {
                        R.string.log_overlay_permission_granted
                    } else {
                        R.string.log_overlay_permission_denied
                    }
                )
            )
        }
    }
    val leftFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val persisted = takePersistableTreePermission(uri)
        val updated = settings.leftDestination.copy(treeUri = uri.toString(), relativePath = null, enabled = true)
        scope.launch {
            if (persisted) {
                saveDestination(updated)
                appendLog(context.getString(R.string.log_left_destination_saf))
            } else {
                appendLog(context.getString(R.string.log_saf_permission_not_persisted))
            }
        }
    }
    val rightFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val persisted = takePersistableTreePermission(uri)
        val updated = settings.rightDestination.copy(treeUri = uri.toString(), relativePath = null, enabled = true)
        scope.launch {
            if (persisted) {
                saveDestination(updated)
                appendLog(context.getString(R.string.log_right_destination_saf))
            } else {
                appendLog(context.getString(R.string.log_saf_permission_not_persisted))
            }
        }
    }

    val notificationGranted = remember(permissionRefresh) { PermissionChecks.hasNotificationPermission(context) }
    val fullMediaGranted = remember(permissionRefresh) { PermissionChecks.hasFullImageReadAccess(context) }
    val partialMediaGranted = remember(permissionRefresh) { PermissionChecks.hasPartialVisualAccess(context) }
    val overlayGranted = remember(permissionRefresh) { OverlayPermission.canDrawOverlays(context) }
    val routeUiAvailable = overlayGranted || notificationGranted
    val canStartMonitoring = fullMediaGranted && routeUiAvailable
    val monitorUnavailableReason = monitoringUnavailableReason(
        fullMediaGranted = fullMediaGranted,
        partialMediaGranted = partialMediaGranted,
        routeUiAvailable = routeUiAvailable
    )

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Text(tab.label.first().toString()) },
                            label = { Text(tabLabel(tab)) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                MainTab.Setup -> SetupChecklistScreen(
                    modifier = Modifier.padding(padding),
                    notificationGranted = notificationGranted,
                    fullMediaGranted = fullMediaGranted,
                    partialMediaGranted = partialMediaGranted,
                    overlayGranted = overlayGranted,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestMedia = { mediaLauncher.launch(PermissionChecks.mediaPermissionsForRequest()) },
                    onOpenOverlaySettings = { overlayLauncher.launch(OverlayPermission.settingsIntent(context)) }
                )
                MainTab.Destinations -> DestinationMappingScreen(
                    modifier = Modifier.padding(padding),
                    settings = settings,
                    onSaveDestination = { destination -> scope.launch { saveDestination(destination) } },
                    onChooseLeftFolder = { leftFolderLauncher.launch(null) },
                    onChooseRightFolder = { rightFolderLauncher.launch(null) },
                    appManagedAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    onUseAppManaged = { direction ->
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            scope.launch { appendLog(context.getString(R.string.log_app_managed_requires_api29)) }
                        } else {
                            val destination = when (direction) {
                                SwipeDirection.Left -> settings.leftDestination.copy(
                                    treeUri = null,
                                    relativePath = AppManagedPaths.forSwipeDirection(direction),
                                    enabled = true
                                )
                                SwipeDirection.Right -> settings.rightDestination.copy(
                                    treeUri = null,
                                    relativePath = AppManagedPaths.forSwipeDirection(direction),
                                    enabled = true
                                )
                                else -> null
                            }
                            destination?.let {
                                scope.launch {
                                    saveDestination(it)
                                    appendLog(context.getString(R.string.log_destination_app_managed, direction.name))
                                }
                            }
                        }
                    },
                    onRouteModeChange = { mode -> scope.launch { setRouteMode(mode) } }
                )
                MainTab.Monitor -> MonitorScreen(
                    modifier = Modifier.padding(padding),
                    settings = settings,
                    notificationGranted = notificationGranted,
                    fullMediaGranted = fullMediaGranted,
                    partialMediaGranted = partialMediaGranted,
                    overlayGranted = overlayGranted,
                    canStart = canStartMonitoring,
                    unavailableReason = monitorUnavailableReason,
                    latestLog = logs.firstOrNull(),
                    onStart = {
                        if (canStartMonitoring) {
                            startMonitoring()
                        } else {
                            scope.launch {
                                appendLog(monitorUnavailableReason ?: context.getString(R.string.monitoring_blocked_generic))
                            }
                        }
                    },
                    onStop = stopMonitoring
                )
                MainTab.Log -> LocalLogScreen(
                    modifier = Modifier.padding(padding),
                    entries = logs,
                    onClear = { scope.launch { clearLog() } }
                )
            }
        }
    }
}

@Composable
private fun monitoringUnavailableReason(
    fullMediaGranted: Boolean,
    partialMediaGranted: Boolean,
    routeUiAvailable: Boolean
): String? = when {
    !fullMediaGranted && partialMediaGranted -> stringResource(R.string.monitoring_blocked_partial_media)
    !fullMediaGranted -> stringResource(R.string.monitoring_blocked_missing_full_media)
    !routeUiAvailable -> stringResource(R.string.monitoring_blocked_no_route_ui)
    else -> null
}

@Composable
private fun tabLabel(tab: MainTab): String = when (tab) {
    MainTab.Setup -> stringResource(R.string.tab_setup)
    MainTab.Destinations -> stringResource(R.string.tab_destinations)
    MainTab.Monitor -> stringResource(R.string.tab_monitor)
    MainTab.Log -> stringResource(R.string.tab_log)
}
