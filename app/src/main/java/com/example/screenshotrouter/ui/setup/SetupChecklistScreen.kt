package com.example.screenshotrouter.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenshotrouter.R

@Composable
fun SetupChecklistScreen(
    modifier: Modifier = Modifier,
    notificationGranted: Boolean,
    fullMediaGranted: Boolean,
    partialMediaGranted: Boolean,
    overlayGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestMedia: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.permission_local_only), style = MaterialTheme.typography.bodyMedium)

        PermissionCard(
            title = stringResource(R.string.permission_notifications_title),
            granted = notificationGranted,
            detail = stringResource(R.string.permission_notifications_detail),
            actionLabel = stringResource(R.string.action_grant_notifications),
            onAction = onRequestNotifications
        )
        PermissionCard(
            title = stringResource(R.string.permission_media_title),
            granted = fullMediaGranted,
            detail = if (partialMediaGranted) {
                stringResource(R.string.permission_media_partial_detail)
            } else {
                stringResource(R.string.permission_media_detail)
            },
            actionLabel = if (partialMediaGranted) {
                stringResource(R.string.action_request_full_image_access)
            } else {
                stringResource(R.string.action_grant_image_access)
            },
            onAction = onRequestMedia
        )
        PermissionCard(
            title = stringResource(R.string.permission_overlay_title),
            granted = overlayGranted,
            detail = stringResource(R.string.permission_overlay_detail),
            actionLabel = stringResource(R.string.action_open_overlay_settings),
            onAction = onOpenOverlaySettings,
            optional = true
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    optional: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    if (granted) {
                        stringResource(R.string.permission_granted)
                    } else if (optional) {
                        stringResource(R.string.permission_optional)
                    } else {
                        stringResource(R.string.permission_needed)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(detail)
            Spacer(Modifier.height(12.dp))
            if (!granted) {
                if (optional) {
                    OutlinedButton(onClick = onAction) { Text(actionLabel) }
                } else {
                    Button(onClick = onAction) { Text(actionLabel) }
                }
            }
        }
    }
}
