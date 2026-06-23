package com.example.screenshotrouter.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.WindowManager
import com.example.screenshotrouter.core.model.ScreenshotEvent
import com.example.screenshotrouter.core.model.SwipeDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotOverlayController(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentView: ScreenshotOverlayView? = null
    private var timeoutRunnable: Runnable? = null

    fun show(
        event: ScreenshotEvent,
        onSwipe: (SwipeDirection) -> Unit
    ): Boolean {
        if (!OverlayPermission.canDrawOverlays(appContext)) return false
        dismiss()

        val view = ScreenshotOverlayView(appContext).apply {
            bind(event)
            setCloseAction { dismiss() }
            setOnTouchListener(SwipeGestureDetector(appContext) { direction ->
                dismiss()
                onSwipe(direction)
            })
        }

        val params = WindowManager.LayoutParams(
            dp(340),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(56)
            setTitle("ScreenshotRouter")
        }

        return runCatching {
            windowManager.addView(view, params)
            currentView = view
            loadThumbnail(event, view)
            scheduleTimeout()
            true
        }.getOrElse {
            currentView = null
            false
        }
    }

    fun dismiss() {
        timeoutRunnable?.let(handler::removeCallbacks)
        timeoutRunnable = null
        val view = currentView ?: return
        currentView = null
        runCatching { windowManager.removeView(view) }
    }

    private fun scheduleTimeout() {
        val runnable = Runnable { dismiss() }
        timeoutRunnable = runnable
        handler.postDelayed(runnable, OVERLAY_TIMEOUT_MILLIS)
    }

    private fun loadThumbnail(event: ScreenshotEvent, view: ScreenshotOverlayView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        scope.launch(Dispatchers.IO) {
            val bitmap: Bitmap? = runCatching {
                appContext.contentResolver.loadThumbnail(event.uri, Size(dp(96), dp(96)), null)
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (currentView === view) view.setThumbnail(bitmap)
            }
        }
    }

    private fun dp(value: Int): Int = (value * appContext.resources.displayMetrics.density).toInt()

    companion object {
        const val OVERLAY_TIMEOUT_MILLIS = 5_000L
    }
}
