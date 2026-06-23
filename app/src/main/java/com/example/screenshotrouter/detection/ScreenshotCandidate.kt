package com.example.screenshotrouter.detection

data class ScreenshotCandidate(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val relativePath: String?,
    val dateAddedMillis: Long?,
    val dateTakenMillis: Long?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val isPending: Boolean?
)
