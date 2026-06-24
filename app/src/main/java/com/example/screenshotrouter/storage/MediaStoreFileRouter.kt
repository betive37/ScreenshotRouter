package com.example.screenshotrouter.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.util.FileNameUtils
import com.example.screenshotrouter.detection.RoutedCopyRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class MediaStoreFileRouter(
    private val context: Context
) {
    suspend fun copy(event: ScreenshotEvent, relativePath: String): CopyOutcome = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext CopyOutcome.Failure(
                "App-managed MediaStore destinations require Android 10+; choose a SAF folder on Android 8/9."
            )
        }

        val resolver = context.contentResolver
        val normalizedPath = FileNameUtils.normalizeRelativePath(relativePath)
        val mimeType = event.mimeType ?: "image/png"
        val desiredName = FileNameUtils.safeDisplayName(event.displayName, mimeType)
        val targetName = FileNameUtils.collisionSafeName(desiredName, existingNames(normalizedPath))

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, targetName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, normalizedPath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext CopyOutcome.Failure("Could not create MediaStore destination item")

        var keepDestination = false
        try {
            val input = resolver.openInputStream(event.uri)
                ?: return@withContext CopyOutcome.Failure("Could not open screenshot stream")
            val output = resolver.openOutputStream(destinationUri, "w")
                ?: return@withContext CopyOutcome.Failure("Could not open MediaStore destination stream")
            val bytes = input.use { source -> output.use { sink -> source.copyCountingTo(sink) } }

            if (bytes <= 0L) {
                return@withContext CopyOutcome.Failure("Copied zero bytes; original was left untouched")
            }

            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            val updatedRows = resolver.update(destinationUri, done, null, null)
            if (updatedRows <= 0) {
                return@withContext CopyOutcome.Failure("Could not finalize MediaStore destination item")
            }

            keepDestination = true
            RoutedCopyRegistry.remember(
                id = runCatching { ContentUris.parseId(destinationUri) }.getOrNull(),
                uriString = destinationUri.toString(),
                displayName = targetName,
                relativePath = normalizedPath
            )
            CopyOutcome.Success(destinationUri, bytes, targetName)
        } catch (error: Throwable) {
            CopyOutcome.Failure("MediaStore copy failed: ${error.message ?: error::class.java.simpleName}", error)
        } finally {
            if (!keepDestination) runCatching { resolver.delete(destinationUri, null, null) }
        }
    }

    private fun existingNames(relativePath: String): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptySet()
        val resolver = context.contentResolver
        val names = mutableSetOf<String>()
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.Images.Media.RELATIVE_PATH}=?",
            arrayOf(relativePath),
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (index >= 0 && !cursor.isNull(index)) names += cursor.getString(index)
            }
        }
        return names
    }

    private fun InputStream.copyCountingTo(output: OutputStream): Long {
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            total += read.toLong()
        }
        output.flush()
        return total
    }
}
