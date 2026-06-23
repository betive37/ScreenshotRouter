package com.example.screenshotrouter.storage

import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.SwipeDirection

object DestinationResolver {
    fun findDestination(destinations: List<Destination>, direction: SwipeDirection): Destination? =
        destinations.firstOrNull { it.direction == direction && it.hasWritableTarget }
}
