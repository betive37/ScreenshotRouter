package com.example.screenshotrouter.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoutedCopyRegistryTest {
    @Before
    fun setUp() {
        RoutedCopyRegistry.clear()
    }

    @Test
    fun rememberedUriIsReportedAsRoutedCopy() {
        RoutedCopyRegistry.remember(
            id = 42L,
            uriString = "content://media/external/images/media/42",
            displayName = "Screenshot.png",
            relativePath = "Pictures/ScreenshotRouter/A/",
            nowMillis = 10_000L
        )

        assertTrue(
            RoutedCopyRegistry.isRoutedCopy(
                id = 42L,
                uriString = "content://media/external/images/media/42",
                displayName = "Screenshot.png",
                relativePath = "Pictures/ScreenshotRouter/A/",
                nowMillis = 10_100L
            )
        )
    }

    @Test
    fun rememberedCopyExpires() {
        RoutedCopyRegistry.remember(
            id = 42L,
            uriString = "content://media/external/images/media/42",
            displayName = "Screenshot.png",
            relativePath = "Pictures/ScreenshotRouter/A/",
            nowMillis = 10_000L
        )

        assertFalse(
            RoutedCopyRegistry.isRoutedCopy(
                id = 42L,
                uriString = "content://media/external/images/media/42",
                displayName = "Screenshot.png",
                relativePath = "Pictures/ScreenshotRouter/A/",
                nowMillis = 10_000L + 11 * 60 * 1000L
            )
        )
    }
}
