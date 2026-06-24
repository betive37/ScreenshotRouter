package com.example.screenshotrouter.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotClassifierTest {
    private val now = 1_700_000_000_000L
    private val classifier = ScreenshotClassifier(maxAgeMillis = 15_000L)

    @Test
    fun englishScreenshotNameIsAccepted() {
        val candidate = candidate(displayName = "Screenshot_20240623-123000.png")
        assertTrue(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun koreanScreenshotNameIsAccepted() {
        val candidate = candidate(displayName = "스크린샷_20240623.png")
        assertTrue(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun japaneseScreenshotNameIsAccepted() {
        val candidate = candidate(displayName = "スクリーンショット_20240623.png")
        assertTrue(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun chineseScreenshotNameIsAccepted() {
        assertTrue(classifier.isScreenshot(candidate(displayName = "截图_20240623.png"), now))
        assertTrue(classifier.isScreenshot(candidate(displayName = "螢幕截圖_20240623.png"), now))
    }

    @Test
    fun screenshotRelativePathIsAccepted() {
        val candidate = candidate(displayName = "IMG_0001.png", relativePath = "Pictures/Screenshots/")
        assertTrue(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun oldImageIsRejected() {
        val candidate = candidate(dateAddedMillis = now - 60_000L)
        assertFalse(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun appManagedRoutedCopiesAreRejectedEvenWhenNameLooksLikeScreenshot() {
        val candidate = candidate(
            displayName = "Screenshot_20240623.png",
            relativePath = "Pictures/ScreenshotRouter/A/"
        )
        assertFalse(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun nonImageMimeTypeIsRejected() {
        val candidate = candidate(mimeType = "video/mp4")
        assertFalse(classifier.isScreenshot(candidate, now))
    }

    @Test
    fun pendingOrZeroByteImageIsRejected() {
        assertFalse(classifier.isScreenshot(candidate(isPending = true), now))
        assertFalse(classifier.isScreenshot(candidate(sizeBytes = 0L), now))
    }

    private fun candidate(
        displayName: String = "Screenshot.png",
        relativePath: String? = "DCIM/Screenshots/",
        dateAddedMillis: Long? = now - 1_000L,
        mimeType: String? = "image/png",
        sizeBytes: Long? = 10_000L,
        isPending: Boolean? = false
    ) = ScreenshotCandidate(
        id = 1L,
        uriString = "content://media/external/images/media/1",
        displayName = displayName,
        relativePath = relativePath,
        dateAddedMillis = dateAddedMillis,
        dateTakenMillis = null,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        isPending = isPending
    )
}
