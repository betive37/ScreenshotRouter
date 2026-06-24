package com.example.screenshotrouter.core.util

import com.example.screenshotrouter.core.model.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppManagedPathsTest {
    @Test
    fun leftAndRightPathsAreStable() {
        assertEquals("Pictures/ScreenshotRouter/B", AppManagedPaths.forSwipeDirection(SwipeDirection.Left))
        assertEquals("Pictures/ScreenshotRouter/A", AppManagedPaths.forSwipeDirection(SwipeDirection.Right))
    }

    @Test
    fun appManagedRootCheckNormalizesSeparatorsAndCase() {
        assertTrue(AppManagedPaths.isUnderAppManagedRoot("Pictures/ScreenshotRouter/A/"))
        assertTrue(AppManagedPaths.isUnderAppManagedRoot("pictures\\screenshotrouter\\b"))
        assertFalse(AppManagedPaths.isUnderAppManagedRoot("Pictures/Screenshots/"))
    }
}
