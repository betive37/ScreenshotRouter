package com.example.screenshotrouter.core.model

data class AppSettings(
    val leftDestination: Destination = defaultDestination(SwipeDirection.Left),
    val rightDestination: Destination = defaultDestination(SwipeDirection.Right),
    val routeMode: RouteMode = RouteMode.Copy,
    val monitoringActive: Boolean = false
) {
    val destinations: List<Destination> = listOf(leftDestination, rightDestination)

    companion object {
        fun defaultDestination(direction: SwipeDirection): Destination {
            val id = direction.name.lowercase()
            val label = when (direction) {
                SwipeDirection.Left -> "Destination B"
                SwipeDirection.Right -> "Destination A"
                SwipeDirection.Up -> "Destination Up"
                SwipeDirection.Down -> "Destination Down"
            }
            return Destination(
                id = id,
                label = label,
                direction = direction,
                treeUri = null,
                relativePath = null,
                enabled = false
            )
        }
    }
}
