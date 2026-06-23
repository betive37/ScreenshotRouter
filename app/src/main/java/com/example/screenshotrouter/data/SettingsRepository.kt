package com.example.screenshotrouter.data

import com.example.screenshotrouter.core.model.AppSettings
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.RouteMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveDestination(destination: Destination)
    suspend fun setRouteMode(mode: RouteMode)
    suspend fun setMonitoringActive(active: Boolean)
}
