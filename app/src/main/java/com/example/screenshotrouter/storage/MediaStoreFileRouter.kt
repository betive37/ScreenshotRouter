package com.example.screenshotrouter.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.util.FileNameUtils
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

        runCatching {
            val bytes = resolver.openInputStream(event.uri).useNullable { input ->
                resolver.openOutputStream(destinationUri, "w").useNullable { output ->
                    input.copyCountingTo(output)
                }
            } ?: return@withContext CopyOutcome.Failure("Could not open screenshot or MediaStore destination stream")

            if (bytes <= 0L) {
                resolver.delete(destinationUri, null, null)
                return@withContext CopyOutcome.Failure("Copied zero bytes; original was left untouched")
            }

            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(destinationUri, done, null, null)
            CopyOutcome.Success(destinationUri, bytes, targetName)
        }.getOrElse { error ->
            runCatching { resolver.delete(destinationUri, null, null) }
            CopyOutcome.Failure("MediaStore copy failed: ${error.message ?: error::class.java.simpleName}", error)
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

    private inline fun <T : java.io.Closeable, R> T?.useNullable(block: (T) -> R): R? = this?.use(block)

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
