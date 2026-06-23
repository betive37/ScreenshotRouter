package com.example.screenshotrouter.detection

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.util.SystemTimeProvider
import com.example.screenshotrouter.core.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotDetector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val classifier: ScreenshotClassifier = ScreenshotClassifier(),
    private val deduplicator: ScreenshotDeduplicator = ScreenshotDeduplicator(),
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val onDetected: suspend (ScreenshotEvent) -> Unit,
    private val onStatus: suspend (String) -> Unit = {}
) {
    private val resolver: ContentResolver = context.contentResolver
    private var observer: ScreenshotContentObserver? = null
    private var scanJob: Job? = null

    fun start() {
        if (observer != null) return
        val handler = Handler(Looper.getMainLooper())
        observer = ScreenshotContentObserver(handler) { scheduleScan() }
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer!!
        )
        scope.launch { onStatus("MediaStore screenshot observer registered") }
        scheduleScan()
    }

    fun stop() {
        scanJob?.cancel()
        observer?.let { resolver.unregisterContentObserver(it) }
        observer = null
        scope.launch { onStatus("MediaStore screenshot observer unregistered") }
    }

    private fun scheduleScan() {
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(DEBOUNCE_MILLIS)
            runCatching { scanRecentImages() }
                .onFailure { onStatus("Screenshot scan failed: ${it.message ?: it::class.java.simpleName}") }
        }
    }

    private suspend fun scanRecentImages() {
        val candidates = withContext(Dispatchers.IO) { queryRecentCandidates() }
        val now = timeProvider.nowMillis()
        for ((candidate, event) in candidates) {
            if (!classifier.isScreenshot(candidate, now)) continue
            if (deduplicator.isDuplicate(candidate.id, candidate.uriString, now)) continue
            onStatus("Detected screenshot-like image: ${event.displayName}")
            onDetected(event)
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryRecentCandidates(): List<Pair<ScreenshotCandidate, ScreenshotEvent>> {
        val projection = buildProjection()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val out = mutableListOf<Pair<ScreenshotCandidate, ScreenshotEvent>>()

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            var scanned = 0
            while (cursor.moveToNext() && scanned < QUERY_LIMIT) {
                scanned++
                val id = cursor.getLongOrNull(MediaStore.Images.Media._ID) ?: continue
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val displayName = cursor.getStringOrNull(MediaStore.Images.Media.DISPLAY_NAME) ?: "screenshot"
                val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getStringOrNull(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    null
                }
                val dateAddedMillis = cursor.getLongOrNull(MediaStore.Images.Media.DATE_ADDED)?.let { seconds ->
                    if (seconds > 0) seconds * 1000L else null
                }
                val dateTakenMillis = cursor.getLongOrNull(MediaStore.Images.Media.DATE_TAKEN)?.takeIf { it > 0L }
                val mimeType = cursor.getStringOrNull(MediaStore.Images.Media.MIME_TYPE)
                val size = cursor.getLongOrNull(MediaStore.Images.Media.SIZE)
                val isPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getIntOrNull(MediaStore.Images.Media.IS_PENDING)?.let { it == 1 }
                } else {
                    null
                }

                val candidate = ScreenshotCandidate(
                    id = id,
                    uriString = uri.toString(),
                    displayName = displayName,
                    relativePath = relativePath,
                    dateAddedMillis = dateAddedMillis,
                    dateTakenMillis = dateTakenMillis,
                    mimeType = mimeType,
                    sizeBytes = size,
                    isPending = isPending
                )
                val event = ScreenshotEvent(
                    id = id,
                    uri = uri,
                    displayName = displayName,
                    relativePath = relativePath,
                    dateAddedMillis = dateAddedMillis,
                    dateTakenMillis = dateTakenMillis,
                    mimeType = mimeType,
                    sizeBytes = size
                )
                out += candidate to event
            }
        }
        return out
    }

    private fun buildProjection(): Array<String> = buildList {
        add(MediaStore.Images.Media._ID)
        add(MediaStore.Images.Media.DISPLAY_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Images.Media.RELATIVE_PATH)
        add(MediaStore.Images.Media.DATE_ADDED)
        add(MediaStore.Images.Media.DATE_TAKEN)
        add(MediaStore.Images.Media.MIME_TYPE)
        add(MediaStore.Images.Media.SIZE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Images.Media.IS_PENDING)
    }.toTypedArray()

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.getLongOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getIntOrNull(column: String): Int? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getInt(index) else null
    }

    companion object {
        const val DEBOUNCE_MILLIS = 900L
        const val QUERY_LIMIT = 30
    }
}
