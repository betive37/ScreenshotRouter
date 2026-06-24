package com.example.screenshotrouter.monitoring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.screenshotrouter.MainActivity
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.RouteResult
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.monitoring.ScreenshotEventExtras.putScreenshotEvent

class MonitorNotificationController(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            context.getString(R.string.notification_channel_monitoring_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_monitoring_description)
        }
        val routeChannel = NotificationChannel(
            ROUTE_CHANNEL_ID,
            context.getString(R.string.notification_channel_routing_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_routing_description)
        }
        systemManager.createNotificationChannel(monitorChannel)
        systemManager.createNotificationChannel(routeChannel)
    }

    fun buildMonitoringNotification(): Notification {
        ensureChannels()
        return NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_screenshot_router)
            .setContentTitle(context.getString(R.string.notification_monitoring_title))
            .setContentText(context.getString(R.string.notification_monitoring_text))
            .setContentIntent(activityIntent())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_stat_screenshot_router,
                context.getString(R.string.action_stop),
                serviceIntent(ScreenshotMonitorService.ACTION_STOP, NotificationIds.REQUEST_STOP)
            )
            .build()
    }

    @SuppressLint("MissingPermission")
    fun showRouteFallback(event: ScreenshotEvent) {
        ensureChannels()
        val notification = NotificationCompat.Builder(context, ROUTE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_screenshot_router)
            .setContentTitle(context.getString(R.string.notification_route_title))
            .setContentText(event.displayName)
            .setContentIntent(activityIntent())
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_stat_screenshot_router,
                context.getString(R.string.action_save_to_b),
                routeIntent(ScreenshotMonitorService.ACTION_ROUTE_LEFT, event)
            )
            .addAction(
                R.drawable.ic_stat_screenshot_router,
                context.getString(R.string.action_save_to_a),
                routeIntent(ScreenshotMonitorService.ACTION_ROUTE_RIGHT, event)
            )
            .addAction(
                R.drawable.ic_stat_screenshot_router,
                context.getString(R.string.action_dismiss),
                routeIntent(ScreenshotMonitorService.ACTION_DISMISS, event)
            )
            .build()
        runCatching { notificationManager.notify(NotificationIds.routeNotificationId(event), notification) }
    }

    @SuppressLint("MissingPermission")
    fun showRouteResult(result: RouteResult) {
        ensureChannels()
        val message = RouteResultFormatter.format(context, result)
        val builder = NotificationCompat.Builder(context, ROUTE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_screenshot_router)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(activityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        runCatching { notificationManager.notify(NotificationIds.ROUTE_RESULT_NOTIFICATION_ID, builder.build()) }
    }

    @SuppressLint("MissingPermission")
    fun showDeleteConsentRequired(event: ScreenshotEvent, destinationLabel: String) {
        ensureChannels()
        val result = RouteResult.DeleteConsentRequired(destinationLabel)
        val message = RouteResultFormatter.format(context, result)
        val notification = NotificationCompat.Builder(context, ROUTE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_screenshot_router)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(activityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_stat_screenshot_router,
                context.getString(R.string.action_approve_delete),
                deleteConsentIntent(event, destinationLabel)
            )
            .build()
        runCatching { notificationManager.notify(NotificationIds.ROUTE_RESULT_NOTIFICATION_ID, notification) }
    }

    fun cancelRouteNotification(event: ScreenshotEvent?) {
        event ?: return
        runCatching { notificationManager.cancel(NotificationIds.routeNotificationId(event)) }
    }

    private fun activityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            NotificationIds.REQUEST_OPEN_ACTIVITY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ScreenshotMonitorService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun routeIntent(action: String, event: ScreenshotEvent): PendingIntent {
        val intent = Intent(context, ScreenshotMonitorService::class.java)
            .setAction(action)
            .putScreenshotEvent(event)
        return PendingIntent.getService(
            context,
            NotificationIds.routeActionRequestCode(event, action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun deleteConsentIntent(event: ScreenshotEvent, destinationLabel: String): PendingIntent {
        val intent = DeleteConsentActivity.intent(context, event, destinationLabel)
        return PendingIntent.getActivity(
            context,
            NotificationIds.deleteConsentRequestCode(event),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val MONITOR_CHANNEL_ID = "monitoring"
        private const val ROUTE_CHANNEL_ID = "routing"
    }
}
