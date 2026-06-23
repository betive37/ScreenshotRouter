package com.example.screenshotrouter.storage

import android.content.Context
import android.net.Uri
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.RouteResult
import com.example.screenshotrouter.core.model.ScreenshotEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenshotRouter(
    context: Context
) {
    private val appContext = context.applicationContext
    private val safRouter = SafFileRouter(appContext)
    private val mediaStoreRouter = MediaStoreFileRouter(appContext)

    suspend fun route(
        event: ScreenshotEvent,
        destination: Destination,
        routeMode: RouteMode
    ): RouteResult {
        if (!destination.hasWritableTarget) {
            return RouteResult.Failed("No configured destination for ${destination.direction.name.lowercase()} swipe")
        }

        val copyOutcome = when {
            !destination.treeUri.isNullOrBlank() -> safRouter.copy(event, Uri.parse(destination.treeUri))
            !destination.relativePath.isNullOrBlank() -> mediaStoreRouter.copy(event, destination.relativePath)
            else -> CopyOutcome.Failure("Destination has no writable target")
        }

        return when (copyOutcome) {
            is CopyOutcome.Failure -> RouteResult.Failed(copyOutcome.reason, copyOutcome.cause)
            is CopyOutcome.Success -> {
                val deleteStatus = if (routeMode == RouteMode.Move) {
                    tryDeleteOriginal(event)
                } else {
                    DeleteStatus.NotAttempted
                }
                RouteDecision.afterVerifiedCopy(routeMode, destination.label, deleteStatus)
            }
        }
    }

    private suspend fun tryDeleteOriginal(event: ScreenshotEvent): DeleteStatus = withContext(Dispatchers.IO) {
        runCatching {
            val deletedRows = appContext.contentResolver.delete(event.uri, null, null)
            if (deletedRows > 0) DeleteStatus.Deleted else DeleteStatus.NotAllowed
        }.getOrElse { error ->
            when (error) {
                is SecurityException -> DeleteStatus.NeedsUserPermission
                else -> DeleteStatus.Failed(error.message ?: error::class.java.simpleName)
            }
        }
    }
}
