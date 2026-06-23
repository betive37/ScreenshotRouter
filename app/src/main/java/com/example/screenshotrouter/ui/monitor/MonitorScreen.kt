package com.example.screenshotrouter.ui.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.AppSettings
import com.example.screenshotrouter.core.model.LogEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun MonitorScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    notificationGranted: Boolean,
    fullMediaGranted: Boolean,
    partialMediaGranted: Boolean,
    overlayGranted: Boolean,
    latestLog: LogEntry?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.monitor_title), style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.status_title), fontWeight = FontWeight.Bold)
                Text(
                    if (settings.monitoringActive) {
                        stringResource(R.string.monitoring_active)
                    } else {
                        stringResource(R.string.monitoring_stopped)
                    }
                )
                Text(
                    stringResource(
                        R.string.notifications_status,
                        if (notificationGranted) stringResource(R.string.state_granted_lower) else stringResource(R.string.state_not_granted)
                    )
                )
                Text(stringResource(R.string.image_access_status, mediaState(fullMediaGranted, partialMediaGranted)))
                Text(
                    stringResource(
                        R.string.overlay_status,
                        if (overlayGranted) stringResource(R.string.overlay_available) else stringResource(R.string.overlay_fallback)
                    )
                )
                Text(
                    stringResource(
                        R.string.left_destination_status,
                        if (settings.leftDestination.hasWritableTarget) settings.leftDestination.label else stringResource(R.string.not_configured)
                    )
                )
                Text(
                    stringResource(
                        R.string.right_destination_status,
                        if (settings.rightDestination.hasWritableTarget) settings.rightDestination.label else stringResource(R.string.not_configured)
                    )
                )
                Text(stringResource(R.string.mode_status, routeModeLabel(settings.routeMode)))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart, enabled = !settings.monitoringActive) { Text(stringResource(R.string.action_start_monitoring)) }
            OutlinedButton(onClick = onStop, enabled = settings.monitoringActive) { Text(stringResource(R.string.action_stop)) }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.latest_status_title), fontWeight = FontWeight.Bold)
                if (latestLog == null) {
                    Text(stringResource(R.string.no_local_events))
                } else {
                    Text(DateFormat.getDateTimeInstance().format(Date(latestLog.timestampMillis)))
                    Text(latestLog.message)
                }
            }
        }
    }
}

@Composable
private fun mediaState(full: Boolean, partial: Boolean): String = when {
    full -> stringResource(R.string.media_state_full)
    partial -> stringResource(R.string.media_state_partial)
    else -> stringResource(R.string.media_state_missing)
}

@Composable
private fun routeModeLabel(routeMode: com.example.screenshotrouter.core.model.RouteMode): String = when (routeMode) {
    com.example.screenshotrouter.core.model.RouteMode.Copy -> stringResource(R.string.route_mode_copy)
    com.example.screenshotrouter.core.model.RouteMode.Move -> stringResource(R.string.route_mode_move)
}
