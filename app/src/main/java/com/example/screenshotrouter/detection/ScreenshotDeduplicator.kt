package com.example.screenshotrouter.detection

class ScreenshotDeduplicator(
    private val duplicateWindowMillis: Long = DEFAULT_DUPLICATE_WINDOW_MILLIS,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    private val seen = LinkedHashMap<String, Long>()

    @Synchronized
    fun isDuplicate(id: Long, uriString: String, nowMillis: Long): Boolean {
        purgeExpired(nowMillis)
        val key = if (id > 0L) "id:$id" else "uri:$uriString"
        val previous = seen[key]
        seen[key] = nowMillis
        trimToMaxEntries()
        return previous != null && nowMillis - previous <= duplicateWindowMillis
    }

    @Synchronized
    fun clear() {
        seen.clear()
    }

    private fun purgeExpired(nowMillis: Long) {
        val iterator = seen.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowMillis - entry.value > duplicateWindowMillis) {
                iterator.remove()
            }
        }
    }

    private fun trimToMaxEntries() {
        while (seen.size > maxEntries) {
            val firstKey = seen.entries.firstOrNull()?.key ?: return
            seen.remove(firstKey)
        }
    }

    companion object {
        const val DEFAULT_DUPLICATE_WINDOW_MILLIS = 5 * 60 * 1000L
        const val DEFAULT_MAX_ENTRIES = 128
    }
}
