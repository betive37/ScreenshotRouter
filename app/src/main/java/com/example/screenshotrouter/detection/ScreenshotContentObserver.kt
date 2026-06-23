package com.example.screenshotrouter.detection

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class ScreenshotContentObserver(
    handler: Handler,
    private val onChanged: () -> Unit
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        onChanged()
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        onChanged()
    }

    override fun onChange(selfChange: Boolean, uris: Collection<Uri>, flags: Int) {
        onChanged()
    }
}
