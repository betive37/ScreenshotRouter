package com.example.screenshotrouter.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameUtilsTest {
    @Test
    fun addsExtensionFromMimeTypeWhenMissing() {
        assertEquals("Screenshot.jpg", FileNameUtils.safeDisplayName("Screenshot", "image/jpeg"))
    }

    @Test
    fun preservesExistingExtension() {
        assertEquals("Screenshot.png", FileNameUtils.safeDisplayName("Screenshot.png", "image/jpeg"))
    }

    @Test
    fun appendsCounterForCollisions() {
        val existing = setOf("Screenshot.png", "Screenshot_1.png", "Screenshot_2.png")
        assertEquals("Screenshot_3.png", FileNameUtils.collisionSafeName("Screenshot.png", existing))
    }

    @Test
    fun normalizesRelativePath() {
        assertEquals("Pictures/ScreenshotRouter/A/", FileNameUtils.normalizeRelativePath("Pictures//ScreenshotRouter/A"))
    }
}
