package com.example.screenshotrouter.core.model

sealed interface RouteResult {
    data class Copied(val destinationLabel: String) : RouteResult
    data class Moved(val destinationLabel: String) : RouteResult
    data class DeleteConsentRequired(val destinationLabel: String) : RouteResult
    data class NeedsUserPermission(val reason: String) : RouteResult
    data class Failed(val reason: String, val cause: Throwable? = null) : RouteResult
}

fun RouteResult.userMessage(): String = when (this) {
    is RouteResult.Copied -> "Copied to $destinationLabel"
    is RouteResult.Moved -> "Moved to $destinationLabel"
    is RouteResult.DeleteConsentRequired -> "Copied to $destinationLabel; approve Android's delete request to remove the original."
    is RouteResult.NeedsUserPermission -> reason
    is RouteResult.Failed -> reason
}
