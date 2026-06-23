package com.example.screenshotrouter.core.util

import java.util.Locale

object FileNameUtils {
    private val extensionByMime = mapOf(
        "image/png" to "png",
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/webp" to "webp",
        "image/heic" to "heic",
        "image/heif" to "heif"
    )

    fun safeDisplayName(displayName: String?, mimeType: String?): String {
        val base = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(Regex("[\\r\\n\\t]"), "_")
            ?.replace(Regex("[\\u0000-\\u001F]"), "_")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "screenshot"

        val sanitized = base.replace('/', '_').replace('\\', '_')
        if (sanitized.substringAfterLast('.', missingDelimiterValue = "").isNotBlank() && sanitized.contains('.')) {
            return sanitized
        }

        val ext = extensionByMime[mimeType?.lowercase(Locale.ROOT)] ?: "png"
        return "$sanitized.$ext"
    }

    fun collisionSafeName(desiredName: String, existingNames: Set<String>): String {
        val sanitized = safeDisplayName(desiredName, null)
        if (!existingNames.contains(sanitized)) return sanitized

        val dot = sanitized.lastIndexOf('.').takeIf { it > 0 }
        val stem = dot?.let { sanitized.substring(0, it) } ?: sanitized
        val extension = dot?.let { sanitized.substring(it) } ?: ""

        var index = 1
        while (true) {
            val candidate = "${stem}_$index$extension"
            if (!existingNames.contains(candidate)) return candidate
            index++
        }
    }

    fun normalizeRelativePath(path: String): String {
        val cleaned = path
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
        return if (cleaned.endsWith('/')) cleaned else "$cleaned/"
    }
}
