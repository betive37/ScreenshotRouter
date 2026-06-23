package com.example.screenshotrouter.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.util.FileNameUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SafFileRouter(
    private val context: Context
) {
    suspend fun copy(event: ScreenshotEvent, treeUri: Uri): CopyOutcome = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val tree = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext CopyOutcome.Failure("Selected destination folder is unavailable")
            if (!tree.canWrite()) {
                return@withContext CopyOutcome.Failure("Selected destination folder is not writable")
            }

            val mimeType = event.mimeType ?: "image/png"
            val desiredName = FileNameUtils.safeDisplayName(event.displayName, mimeType)
            val existing = tree.listFiles().mapNotNull { it.name }.toSet()
            val targetName = FileNameUtils.collisionSafeName(desiredName, existing)
            val document = tree.createFile(mimeType, targetName)
                ?: return@withContext CopyOutcome.Failure("Could not create destination file in selected folder")

            val bytes = resolver.openInputStream(event.uri).useNullable { input ->
                resolver.openOutputStream(document.uri, "w").useNullable { output ->
                    input.copyCountingTo(output)
                }
            } ?: return@withContext CopyOutcome.Failure("Could not open screenshot or destination stream")

            if (bytes <= 0L) {
                runCatching { document.delete() }
                return@withContext CopyOutcome.Failure("Copied zero bytes; original was left untouched")
            }

            CopyOutcome.Success(document.uri, bytes, targetName)
        }.getOrElse { error ->
            CopyOutcome.Failure("SAF copy failed: ${error.message ?: error::class.java.simpleName}", error)
        }
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
