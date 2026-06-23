package com.example.screenshotrouter.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.screenshotrouter.R
import com.example.screenshotrouter.core.model.ScreenshotEvent

class ScreenshotOverlayView(context: Context) : FrameLayout(context) {
    private val thumbnail = ImageView(context)
    private val title = TextView(context)
    private val subtitle = TextView(context)
    private val close = ImageButton(context)

    init {
        isClickable = true
        isFocusable = false
        elevation = dp(8).toFloat()
        background = GradientDrawable().apply {
            setColor(Color.argb(240, 32, 32, 32))
            cornerRadius = dp(18).toFloat()
        }
        setPadding(dp(12), dp(10), dp(8), dp(10))

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        thumbnail.layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
            marginEnd = dp(10)
        }
        thumbnail.setBackgroundColor(Color.argb(255, 64, 64, 64))
        thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
        thumbnail.setImageResource(R.drawable.ic_stat_screenshot_router)
        row.addView(thumbnail)

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        title.setTextColor(Color.WHITE)
        title.textSize = 14f
        title.typeface = Typeface.DEFAULT_BOLD
        title.maxLines = 1
        subtitle.setTextColor(Color.argb(230, 255, 255, 255))
        subtitle.textSize = 12f
        subtitle.maxLines = 2
        texts.addView(title)
        texts.addView(subtitle)
        row.addView(texts)

        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        close.setBackgroundColor(Color.TRANSPARENT)
        close.colorFilter = android.graphics.PorterDuffColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
        close.contentDescription = context.getString(R.string.overlay_dismiss_content_description)
        close.layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        row.addView(close)

        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bind(event: ScreenshotEvent) {
        title.text = getContext().getString(R.string.overlay_route_screenshot)
        subtitle.text = getContext().getString(R.string.overlay_route_hint, event.displayName.take(24))
    }

    fun setThumbnail(bitmap: Bitmap?) {
        if (bitmap != null) thumbnail.setImageBitmap(bitmap)
    }

    fun setCloseAction(action: () -> Unit) {
        close.setOnClickListener { action() }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
