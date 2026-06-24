package com.example.screenshotrouter.ui.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.AppSettings
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.SwipeDirection

@Composable
fun DestinationMappingScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    onSaveDestination: (Destination) -> Unit,
    onChooseLeftFolder: () -> Unit,
    onChooseRightFolder: () -> Unit,
    appManagedAvailable: Boolean,
    onUseAppManaged: (SwipeDirection) -> Unit,
    onRouteModeChange: (RouteMode) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.destinations_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.destinations_description))

        RouteModeCard(settings.routeMode, onRouteModeChange)

        DestinationCard(
            title = stringResource(R.string.left_swipe_destination_title),
            destination = settings.leftDestination,
            onDestinationChange = onSaveDestination,
            onChooseFolder = onChooseLeftFolder,
            appManagedAvailable = appManagedAvailable,
            onUseAppManaged = { onUseAppManaged(SwipeDirection.Left) }
        )
        DestinationCard(
            title = stringResource(R.string.right_swipe_destination_title),
            destination = settings.rightDestination,
            onDestinationChange = onSaveDestination,
            onChooseFolder = onChooseRightFolder,
            appManagedAvailable = appManagedAvailable,
            onUseAppManaged = { onUseAppManaged(SwipeDirection.Right) }
        )
    }
}

@Composable
private fun RouteModeCard(
    routeMode: RouteMode,
    onRouteModeChange: (RouteMode) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.route_mode_title), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (routeMode == RouteMode.Copy) {
                    Button(onClick = { onRouteModeChange(RouteMode.Copy) }) { Text(stringResource(R.string.route_mode_copy)) }
                    OutlinedButton(onClick = { onRouteModeChange(RouteMode.Move) }) { Text(stringResource(R.string.route_mode_move)) }
                } else {
                    OutlinedButton(onClick = { onRouteModeChange(RouteMode.Copy) }) { Text(stringResource(R.string.route_mode_copy)) }
                    Button(onClick = { onRouteModeChange(RouteMode.Move) }) { Text(stringResource(R.string.route_mode_move)) }
                }
            }
            Text(stringResource(R.string.route_mode_description))
        }
    }
}

@Composable
private fun DestinationCard(
    title: String,
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    onChooseFolder: () -> Unit,
    appManagedAvailable: Boolean,
    onUseAppManaged: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (destination.enabled) {
                            stringResource(R.string.destination_enabled)
                        } else {
                            stringResource(R.string.destination_disabled)
                        }
                    )
                    Switch(
                        checked = destination.enabled,
                        onCheckedChange = { onDestinationChange(destination.copy(enabled = it)) }
                    )
                }
            }

            TextField(
                value = destination.label,
                onValueChange = { onDestinationChange(destination.copy(label = it.ifBlank { destination.label })) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.destination_label)) },
                singleLine = true
            )

            Text(stringResource(R.string.destination_target, destination.describeTarget()))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onChooseFolder) { Text(stringResource(R.string.action_choose_saf_folder)) }
                OutlinedButton(onClick = onUseAppManaged, enabled = appManagedAvailable) {
                    Text(stringResource(R.string.action_use_app_managed))
                }
            }
            if (!appManagedAvailable) {
                Text(stringResource(R.string.app_managed_requires_api29))
            }
            TextButton(
                onClick = {
                    onDestinationChange(destination.copy(treeUri = null, relativePath = null, enabled = false))
                }
            ) { Text(stringResource(R.string.action_clear_destination)) }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun Destination.describeTarget(): String = when {
    !treeUri.isNullOrBlank() -> stringResource(R.string.destination_target_saf)
    !relativePath.isNullOrBlank() -> stringResource(R.string.destination_target_media_store, relativePath)
    else -> stringResource(R.string.destination_target_unconfigured)
}
