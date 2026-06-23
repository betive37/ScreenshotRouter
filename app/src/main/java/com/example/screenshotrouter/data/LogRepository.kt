package com.example.screenshotrouter.data

import com.example.screenshotrouter.core.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    val entries: Flow<List<LogEntry>>
    suspend fun append(message: String)
    suspend fun clear()
}
