package com.example.screenshotrouter.detection

import java.util.Locale

object RoutedCopyRegistry {
    private const val DEFAULT_WINDOW_MILLIS = 10 * 60 * 1000L
    private const val MAX_ENTRIES = 128
    private val entries = LinkedHashMap<String, Long>()

    @Synchronized
    fun remember(
        id: Long?,
        uriString: String?,
        displayName: String?,
        relativePath: String?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        purgeExpired(nowMillis)
        keysFor(id, uriString, displayName, relativePath).forEach { key -> entries[key] = nowMillis }
        trimToMaxEntries()
    }

    @Synchronized
    fun isRoutedCopy(
        id: Long,
        uriString: String,
        displayName: String?,
        relativePath: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        purgeExpired(nowMillis)
        return keysFor(id, uriString, displayName, relativePath).any(entries::containsKey)
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    private fun keysFor(
        id: Long?,
        uriString: String?,
        displayName: String?,
        relativePath: String?
    ): Set<String> = buildSet {
        if (id != null && id > 0L) add("id:$id")
        if (!uriString.isNullOrBlank()) add("uri:${uriString.trim()}")
        val normalizedName = displayName?.trim()?.lowercase(Locale.ROOT)
        val normalizedPath = relativePath?.replace('\\', '/')?.trim('/')?.lowercase(Locale.ROOT)
        if (!normalizedName.isNullOrBlank() && !normalizedPath.isNullOrBlank()) {
            add("name-path:$normalizedPath/$normalizedName")
        }
    }

    private fun purgeExpired(nowMillis: Long) {
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowMillis - entry.value > DEFAULT_WINDOW_MILLIS) iterator.remove()
        }
    }

    private fun trimToMaxEntries() {
        while (entries.size > MAX_ENTRIES) {
            val firstKey = entries.entries.firstOrNull()?.key ?: return
            entries.remove(firstKey)
        }
    }
}
