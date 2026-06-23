package com.example.screenshotrouter.storage

import com.example.screenshotrouter.core.model.Destination
import com.example.screenshotrouter.core.model.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationResolverTest {
    @Test
    fun findsEnabledDestinationByDirection() {
        val left = Destination("left", "B", SwipeDirection.Left, "content://tree/left", null, true)
        val right = Destination("right", "A", SwipeDirection.Right, "content://tree/right", null, true)

        assertEquals(left, DestinationResolver.findDestination(listOf(left, right), SwipeDirection.Left))
        assertEquals(right, DestinationResolver.findDestination(listOf(left, right), SwipeDirection.Right))
    }

    @Test
    fun ignoresDisabledOrUnconfiguredDestination() {
        val disabled = Destination("left", "B", SwipeDirection.Left, "content://tree/left", null, false)
        val missingTarget = Destination("right", "A", SwipeDirection.Right, null, null, true)

        assertNull(DestinationResolver.findDestination(listOf(disabled, missingTarget), SwipeDirection.Left))
        assertNull(DestinationResolver.findDestination(listOf(disabled, missingTarget), SwipeDirection.Right))
    }
}
