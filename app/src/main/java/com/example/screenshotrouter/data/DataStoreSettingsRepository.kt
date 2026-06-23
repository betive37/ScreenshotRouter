package com.example.screenshotrouter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.AppSettings
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.SwipeDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            leftDestination = Destination(
                id = "left",
                label = prefs[Keys.leftLabel] ?: context.getString(R.string.default_destination_b),
                direction = SwipeDirection.Left,
                treeUri = prefs[Keys.leftTreeUri],
                relativePath = prefs[Keys.leftRelativePath],
                enabled = prefs[Keys.leftEnabled] ?: false
            ),
            rightDestination = Destination(
                id = "right",
                label = prefs[Keys.rightLabel] ?: context.getString(R.string.default_destination_a),
                direction = SwipeDirection.Right,
                treeUri = prefs[Keys.rightTreeUri],
                relativePath = prefs[Keys.rightRelativePath],
                enabled = prefs[Keys.rightEnabled] ?: false
            ),
            routeMode = RouteMode.fromName(prefs[Keys.routeMode]),
            monitoringActive = prefs[Keys.monitoringActive] ?: false
        )
    }

    override suspend fun saveDestination(destination: Destination) {
        context.settingsDataStore.edit { prefs ->
            when (destination.direction) {
                SwipeDirection.Left -> {
                    prefs[Keys.leftLabel] = destination.label
                    prefs[Keys.leftEnabled] = destination.enabled
                    if (destination.treeUri.isNullOrBlank()) prefs.remove(Keys.leftTreeUri) else prefs[Keys.leftTreeUri] = destination.treeUri
                    if (destination.relativePath.isNullOrBlank()) prefs.remove(Keys.leftRelativePath) else prefs[Keys.leftRelativePath] = destination.relativePath
                }
                SwipeDirection.Right -> {
                    prefs[Keys.rightLabel] = destination.label
                    prefs[Keys.rightEnabled] = destination.enabled
                    if (destination.treeUri.isNullOrBlank()) prefs.remove(Keys.rightTreeUri) else prefs[Keys.rightTreeUri] = destination.treeUri
                    if (destination.relativePath.isNullOrBlank()) prefs.remove(Keys.rightRelativePath) else prefs[Keys.rightRelativePath] = destination.relativePath
                }
                SwipeDirection.Up,
                SwipeDirection.Down -> Unit
            }
        }
    }

    override suspend fun setRouteMode(mode: RouteMode) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.routeMode] = mode.name }
    }

    override suspend fun setMonitoringActive(active: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.monitoringActive] = active }
    }

    private object Keys {
        val leftLabel = stringPreferencesKey("left_label")
        val leftTreeUri = stringPreferencesKey("left_tree_uri")
        val leftRelativePath = stringPreferencesKey("left_relative_path")
        val leftEnabled = booleanPreferencesKey("left_enabled")

        val rightLabel = stringPreferencesKey("right_label")
        val rightTreeUri = stringPreferencesKey("right_tree_uri")
        val rightRelativePath = stringPreferencesKey("right_relative_path")
        val rightEnabled = booleanPreferencesKey("right_enabled")

        val routeMode = stringPreferencesKey("route_mode")
        val monitoringActive = booleanPreferencesKey("monitoring_active")
    }
}
