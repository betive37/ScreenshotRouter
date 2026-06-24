package com.example.screenshotrouter.storage

import android.content.Context
import android.net.Uri
import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.RouteResult
import com.example.screenshotrouter.core.model.ScreenshotEvent

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
            !destination.treeUri.isNullOrBlank() -> {
                val treeUri = Uri.parse(destination.treeUri)
                if (!SafPermissionVerifier.hasPersistedReadWritePermission(appContext, treeUri)) {
                    return RouteResult.NeedsUserPermission(
                        "Selected destination permission is no longer available; choose the SAF folder again."
                    )
                }
                safRouter.copy(event, treeUri)
            }
            !destination.relativePath.isNullOrBlank() -> mediaStoreRouter.copy(event, destination.relativePath)
            else -> CopyOutcome.Failure("Destination has no writable target")
        }

        return when (copyOutcome) {
            is CopyOutcome.Failure -> RouteResult.Failed(copyOutcome.reason, copyOutcome.cause)
            is CopyOutcome.Success -> {
                val deleteStatus = if (routeMode == RouteMode.Move) {
                    DeleteStatus.NeedsUserPermission
                } else {
                    DeleteStatus.NotAttempted
                }
                RouteDecision.afterVerifiedCopy(routeMode, destination.label, deleteStatus)
            }
        }
    }
}
