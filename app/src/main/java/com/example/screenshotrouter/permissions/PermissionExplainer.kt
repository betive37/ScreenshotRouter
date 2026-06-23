package com.example.screenshotrouter.permissions

object PermissionExplainer {
    const val notifications = "Notifications keep the foreground monitor visible and provide route buttons when the overlay is unavailable."
    const val media = "Image/media access lets the active foreground service observe recent MediaStore image metadata and read a screenshot only after you route it."
    const val overlay = "Display over other apps enables the optional swipe card. Without it, ScreenshotRouter uses notification action buttons instead."
    const val localOnly = "ScreenshotRouter has no INTERNET permission, no analytics, no crash reporting, and no OCR. Routing stays on this device."
}
