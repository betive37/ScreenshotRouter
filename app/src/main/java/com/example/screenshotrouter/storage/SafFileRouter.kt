package com.example.screenshotrouter.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.util.FileNameUtils
import com.example.screenshotrouter.detection.RoutedCopyRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SafFileRouter(
    private val context: Context
) {
    suspend fun copy(event: ScreenshotEvent, treeUri: Uri): CopyOutcome = withContext(Dispatchers.IO) {
        if (!SafPermissionVerifier.hasPersistedReadWritePermission(context, treeUri)) {
            return@withContext CopyOutcome.Failure("Selected folder permission is no longer available; choose the SAF folder again")
        }

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

        var keepDestination = false
        try {
            val input = resolver.openInputStream(event.uri)
                ?: return@withContext CopyOutcome.Failure("Could not open screenshot stream")
            val output = resolver.openOutputStream(document.uri, "w")
                ?: return@withContext CopyOutcome.Failure("Could not open selected-folder destination stream")
            val bytes = input.use { source -> output.use { sink -> source.copyCountingTo(sink) } }

            if (bytes <= 0L) {
                return@withContext CopyOutcome.Failure("Copied zero bytes; original was left untouched")
            }

            keepDestination = true
            RoutedCopyRegistry.remember(
                id = null,
                uriString = document.uri.toString(),
                displayName = targetName,
                relativePath = null
            )
            CopyOutcome.Success(document.uri, bytes, targetName)
        } catch (error: Throwable) {
            CopyOutcome.Failure("SAF copy failed: ${error.message ?: error::class.java.simpleName}", error)
        } finally {
            if (!keepDestination) runCatching { document.delete() }
        }
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
