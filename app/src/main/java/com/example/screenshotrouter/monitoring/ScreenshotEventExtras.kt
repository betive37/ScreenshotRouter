package com.example.screenshotrouter.monitoring

import android.content.Intent
import android.net.Uri
import com.example.screenshotrouter.core.model.ScreenshotEvent

object ScreenshotEventExtras {
    private const val EXTRA_ID = "extra_screenshot_id"
    private const val EXTRA_URI = "extra_screenshot_uri"
    private const val EXTRA_DISPLAY_NAME = "extra_screenshot_display_name"
    private const val EXTRA_RELATIVE_PATH = "extra_screenshot_relative_path"
    private const val EXTRA_DATE_ADDED = "extra_screenshot_date_added"
    private const val EXTRA_DATE_TAKEN = "extra_screenshot_date_taken"
    private const val EXTRA_MIME_TYPE = "extra_screenshot_mime_type"
    private const val EXTRA_SIZE = "extra_screenshot_size"

    fun Intent.putScreenshotEvent(event: ScreenshotEvent): Intent = apply {
        putExtra(EXTRA_ID, event.id)
        putExtra(EXTRA_URI, event.uri.toString())
        putExtra(EXTRA_DISPLAY_NAME, event.displayName)
        putExtra(EXTRA_RELATIVE_PATH, event.relativePath)
        putExtra(EXTRA_DATE_ADDED, event.dateAddedMillis ?: -1L)
        putExtra(EXTRA_DATE_TAKEN, event.dateTakenMillis ?: -1L)
        putExtra(EXTRA_MIME_TYPE, event.mimeType)
        putExtra(EXTRA_SIZE, event.sizeBytes ?: -1L)
    }

    fun Intent.getScreenshotEvent(): ScreenshotEvent? {
        val uriString = getStringExtra(EXTRA_URI) ?: return null
        return ScreenshotEvent(
            id = getLongExtra(EXTRA_ID, -1L),
            uri = Uri.parse(uriString),
            displayName = getStringExtra(EXTRA_DISPLAY_NAME) ?: "screenshot",
            relativePath = getStringExtra(EXTRA_RELATIVE_PATH),
            dateAddedMillis = getLongExtra(EXTRA_DATE_ADDED, -1L).takeIf { it >= 0L },
            dateTakenMillis = getLongExtra(EXTRA_DATE_TAKEN, -1L).takeIf { it >= 0L },
            mimeType = getStringExtra(EXTRA_MIME_TYPE),
            sizeBytes = getLongExtra(EXTRA_SIZE, -1L).takeIf { it >= 0L }
        )
    }
}
