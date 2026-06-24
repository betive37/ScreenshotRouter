package com.example.screenshotrouter.core.util

import com.example.screenshotrouter.core.model.SwipeDirection
import java.util.Locale

object AppManagedPaths {
    const val ROOT = "Pictures/ScreenshotRouter"

    fun forSwipeDirection(direction: SwipeDirection): String = when (direction) {
        SwipeDirection.Left -> "$ROOT/B"
        SwipeDirection.Right -> "$ROOT/A"
        SwipeDirection.Up -> "$ROOT/Up"
        SwipeDirection.Down -> "$ROOT/Down"
    }

    fun isUnderAppManagedRoot(relativePath: String?): Boolean {
        val normalized = relativePath
            ?.replace('\\', '/')
            ?.trim('/')
            ?.lowercase(Locale.ROOT)
            ?: return false
        val root = ROOT.lowercase(Locale.ROOT)
        return normalized == root || normalized.startsWith("$root/")
    }
}
