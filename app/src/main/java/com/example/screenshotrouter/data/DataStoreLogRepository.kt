package com.example.screenshotrouter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.screenshotrouter.core.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreLogRepository(
    private val context: Context,
    private val maxEntries: Int = 80
) : LogRepository {

    override val entries: Flow<List<LogEntry>> = context.localLogDataStore.data.map { prefs ->
        prefs[Keys.entries]
            ?.lineSequence()
            ?.mapNotNull(::decode)
            ?.sortedByDescending { it.timestampMillis }
            ?.toList()
            ?: emptyList()
    }

    override suspend fun append(message: String) {
        val now = System.currentTimeMillis()
        context.localLogDataStore.edit { prefs ->
            val existing = prefs[Keys.entries]
                ?.lineSequence()
                ?.mapNotNull(::decode)
                ?.toList()
                ?: emptyList()
            val updated = (listOf(LogEntry(now, message)) + existing)
                .take(maxEntries)
                .joinToString("\n", transform = ::encode)
            prefs[Keys.entries] = updated
        }
    }

    override suspend fun clear() {
        context.localLogDataStore.edit { it.remove(Keys.entries) }
    }

    private fun encode(entry: LogEntry): String {
        val safeMessage = entry.message
            .replace("\n", " ")
            .replace("|", "¦")
        return "${entry.timestampMillis}|$safeMessage"
    }

    private fun decode(line: String): LogEntry? {
        val splitAt = line.indexOf('|')
        if (splitAt <= 0) return null
        val timestamp = line.substring(0, splitAt).toLongOrNull() ?: return null
        val message = line.substring(splitAt + 1).replace("¦", "|")
        return LogEntry(timestamp, message)
    }

    private object Keys {
        val entries = stringPreferencesKey("entries")
    }
}
