package com.example.screenshotrouter.core.model

import android.net.Uri

data class ScreenshotEvent(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val relativePath: String?,
    val dateAddedMillis: Long?,
    val dateTakenMillis: Long?,
    val mimeType: String?,
    val sizeBytes: Long?
)
