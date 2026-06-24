package com.example.screenshotrouter.detection

import com.example.screenshotrouter.core.util.AppManagedPaths
import java.util.Locale

class ScreenshotClassifier(
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    private val futureClockSkewToleranceMillis: Long = DEFAULT_FUTURE_TOLERANCE_MILLIS
) {
    fun isScreenshot(candidate: ScreenshotCandidate, nowMillis: Long): Boolean {
        if (candidate.isPending == true) return false
        if ((candidate.sizeBytes ?: 0L) <= 0L) return false
        if (!candidate.mimeType.orEmpty().lowercase(Locale.ROOT).startsWith("image/")) return false
        if (AppManagedPaths.isUnderAppManagedRoot(candidate.relativePath)) return false
        if (!isRecent(candidate, nowMillis)) return false
        return hasScreenshotToken(candidate.displayName, candidate.relativePath)
    }

    private fun isRecent(candidate: ScreenshotCandidate, nowMillis: Long): Boolean {
        val eventMillis = listOfNotNull(candidate.dateAddedMillis, candidate.dateTakenMillis).maxOrNull() ?: return false
        val age = nowMillis - eventMillis
        return age in -futureClockSkewToleranceMillis..maxAgeMillis
    }

    fun hasScreenshotToken(displayName: String?, relativePath: String?): Boolean {
        val haystack = listOfNotNull(displayName, relativePath)
            .joinToString("/")
            .lowercase(Locale.ROOT)
            .replace('\\', '/')

        return screenshotTokens.any { token -> haystack.contains(token) }
    }

    companion object {
        const val DEFAULT_MAX_AGE_MILLIS = 15_000L
        const val DEFAULT_FUTURE_TOLERANCE_MILLIS = 2_000L

        private val screenshotTokens = listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "screenshots/",
            "/screenshots",
            "pictures/screenshots",
            "dcim/screenshots",
            "스크린샷",
            "화면 캡처",
            "화면캡처",
            "スクリーンショット",
            "スクショ",
            "画面メモ",
            "截屏",
            "截图",
            "螢幕截圖",
            "屏幕截图",
            "螢幕快照",
            "屏幕快照"
        )
    }
}
