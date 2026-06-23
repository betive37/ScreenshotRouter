package com.example.screenshotrouter.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotDeduplicatorTest {
    @Test
    fun repeatedIdWithinWindowIsDuplicate() {
        val deduplicator = ScreenshotDeduplicator(duplicateWindowMillis = 1_000L)
        assertFalse(deduplicator.isDuplicate(42L, "content://one", nowMillis = 10_000L))
        assertTrue(deduplicator.isDuplicate(42L, "content://one", nowMillis = 10_500L))
    }

    @Test
    fun eventAfterWindowIsNotDuplicate() {
        val deduplicator = ScreenshotDeduplicator(duplicateWindowMillis = 1_000L)
        assertFalse(deduplicator.isDuplicate(42L, "content://one", nowMillis = 10_000L))
        assertFalse(deduplicator.isDuplicate(42L, "content://one", nowMillis = 11_500L))
    }

    @Test
    fun uriIsUsedWhenIdIsMissing() {
        val deduplicator = ScreenshotDeduplicator(duplicateWindowMillis = 1_000L)
        assertFalse(deduplicator.isDuplicate(-1L, "content://one", nowMillis = 10_000L))
        assertTrue(deduplicator.isDuplicate(-1L, "content://one", nowMillis = 10_001L))
    }
}
