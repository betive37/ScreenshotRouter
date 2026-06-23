package com.example.screenshotrouter.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.example.screenshotrouter.core.model.SwipeDirection
import kotlin.math.abs

class SwipeGestureDetector(
    context: Context,
    private val onSwipe: (SwipeDirection) -> Unit
) : View.OnTouchListener {
    private val thresholdPx = 72f * context.resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private var consumed = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                consumed = false
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (consumed) return true
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (abs(dx) >= thresholdPx && abs(dx) > abs(dy) * 1.4f) {
                    consumed = true
                    onSwipe(if (dx < 0) SwipeDirection.Left else SwipeDirection.Right)
                    return true
                }
                view.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }
}
