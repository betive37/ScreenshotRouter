package com.example.screenshotrouter.storage

import com.example.screenshotrouter.core.model.RouteMode
import com.example.screenshotrouter.core.model.RouteResult

object RouteDecision {
    fun afterVerifiedCopy(
        routeMode: RouteMode,
        destinationLabel: String,
        deleteStatus: DeleteStatus
    ): RouteResult = when (routeMode) {
        RouteMode.Copy -> RouteResult.Copied(destinationLabel)
        RouteMode.Move -> when (deleteStatus) {
            DeleteStatus.Deleted -> RouteResult.Moved(destinationLabel)
            DeleteStatus.NotAttempted -> RouteResult.NeedsUserPermission(
                "Copied to $destinationLabel; original was kept because delete was not attempted."
            )
            DeleteStatus.NotAllowed -> RouteResult.NeedsUserPermission(
                "Copied to $destinationLabel; original could not be deleted with current storage permission."
            )
            DeleteStatus.NeedsUserPermission -> RouteResult.DeleteConsentRequired(destinationLabel)
            is DeleteStatus.Failed -> RouteResult.NeedsUserPermission(
                "Copied to $destinationLabel; original delete failed: ${deleteStatus.reason}"
            )
        }
    }
}

sealed interface DeleteStatus {
    data object Deleted : DeleteStatus
    data object NotAttempted : DeleteStatus
    data object NotAllowed : DeleteStatus
    data object NeedsUserPermission : DeleteStatus
    data class Failed(val reason: String) : DeleteStatus
}
