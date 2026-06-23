package com.example.screenshotrouter.core.model

data class Destination(
    val id: String,
    val label: String,
    val direction: SwipeDirection,
    val treeUri: String?,
    val relativePath: String?,
    val enabled: Boolean
) {
    val hasWritableTarget: Boolean = enabled && (!treeUri.isNullOrBlank() || !relativePath.isNullOrBlank())
}
