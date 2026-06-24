package com.example.screenshotrouter.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIdsTest {
    @Test
    fun stableHashIsPositiveAndDeterministic() {
        val first = NotificationIds.stablePositiveHash("42", "content://one", "left")
        val second = NotificationIds.stablePositiveHash("42", "content://one", "left")

        assertTrue(first > 0)
        assertEquals(first, second)
    }

    @Test
    fun actionHashChangesWithAction() {
        val left = NotificationIds.stablePositiveHash("42", "content://one", "left")
        val right = NotificationIds.stablePositiveHash("42", "content://one", "right")

        assertNotEquals(left, right)
    }
}
