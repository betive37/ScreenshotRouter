package com.example.screenshotrouter.monitoring

import com.example.screenshotrouter.core.model.ScreenshotEvent

object NotificationIds {
    const val MONITOR_NOTIFICATION_ID = 1_001
    const val ROUTE_RESULT_NOTIFICATION_ID = 1_002
    const val REQUEST_OPEN_ACTIVITY = 10_001
    const val REQUEST_STOP = 10_002

    private const val ROUTE_NOTIFICATION_BASE = 20_000
    private const val ROUTE_NOTIFICATION_RANGE = 20_000
    private const val ROUTE_ACTION_BASE = 50_000
    private const val ROUTE_ACTION_RANGE = 40_000
    private const val DELETE_CONSENT_BASE = 90_000
    private const val DELETE_CONSENT_RANGE = 9_000

    fun routeNotificationId(event: ScreenshotEvent): Int =
        ROUTE_NOTIFICATION_BASE + stablePositiveHash(event.id.toString(), event.uri.toString()) % ROUTE_NOTIFICATION_RANGE

    fun routeActionRequestCode(event: ScreenshotEvent, action: String): Int =
        ROUTE_ACTION_BASE + stablePositiveHash(event.id.toString(), event.uri.toString(), action) % ROUTE_ACTION_RANGE

    fun deleteConsentRequestCode(event: ScreenshotEvent): Int =
        DELETE_CONSENT_BASE + stablePositiveHash(event.id.toString(), event.uri.toString(), "delete") % DELETE_CONSENT_RANGE

    fun stablePositiveHash(vararg parts: String): Int {
        var hash = FNV_OFFSET_BASIS
        parts.forEach { part ->
            part.encodeToByteArray().forEach { byte ->
                hash = hash xor (byte.toInt() and 0xff)
                hash *= FNV_PRIME
            }
            hash = hash xor 0xff
            hash *= FNV_PRIME
        }
        val positive = hash and Int.MAX_VALUE
        return if (positive == 0) 1 else positive
    }

    private const val FNV_OFFSET_BASIS = -0x7ee3623b
    private const val FNV_PRIME = 0x01000193
}
