package com.example.movie_booking_app.view.widget

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Custom View that renders a cinema-screen light projection effect using Canvas.
 *
 * Draws two elements:
 * 1. A soft trapezoid glow (top 88% wide → bottom 100% wide) using LinearGradient
 *    and BlurMaskFilter for a realistic light-cast feel without hard edges.
 * 2. A thin, softly glowing gold line at the top representing the screen edge.
 */
class CinemaScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Reduced from 48f to 24f for a sharper, cleaner light projection
        maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFC107")
        strokeWidth = 4f
        strokeCap = Paint.Cap.BUTT
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val solidLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFC107")
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.BUTT
    }

    private val trapPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val lineY = 8f

        // ADJUST PROPORTIONS TO NARROW THE LIGHT BEAM:
        // Increased from 6% to 12% to shorten the LED bar and center the light beam
        val topInset = w * 0.12f
        val topLeft = topInset
        val topRight = w - topInset

        // Bottom of the trapezoid is no longer 100% wide; inset by 4% on each side
        val bottomInset = w * 0.04f
        val bottomLeft = bottomInset
        val bottomRight = w - bottomInset

        // Length of the light beam (vertical coverage): matches view height
        val bottom = h.toFloat()

        trapPath.reset()
        trapPath.moveTo(topLeft, lineY)
        trapPath.lineTo(topRight, lineY)
        trapPath.lineTo(bottomRight, bottom)
        trapPath.lineTo(bottomLeft, bottom)
        trapPath.close()

        // Tweak Gradient so light is concentrated at the top and fades faster towards the bottom
        glowPaint.shader = LinearGradient(
            w / 2f, lineY, w / 2f, bottom,
            intArrayOf(
                Color.argb(140, 255, 193, 7),   // Slightly reduced peak opacity (from 166 to 140)
                Color.argb(45,  255, 193, 7),   // Fade faster in the middle
                Color.argb(0,   255, 193, 7)    // Completely invisible at the bottom
            ),
            floatArrayOf(0f, 0.35f, 1f), // Shift fade point to 0.35f instead of 0.5f
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawPath(trapPath, glowPaint)

        val lineY = 8f
        // Synchronize LED bar length with the top width of the new light beam
        val topInset = width * 0.12f
        canvas.drawLine(topInset, lineY, width - topInset, lineY, linePaint)
        canvas.drawLine(topInset, lineY, width - topInset, lineY, solidLinePaint)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
}