package com.example.screenshotrouter.storage

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream

class MediaStoreScreenshotReader(
    private val contentResolver: ContentResolver
) {
    fun open(uri: Uri): InputStream? = contentResolver.openInputStream(uri)
}
