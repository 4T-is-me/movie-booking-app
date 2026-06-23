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
class cinemaScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Giảm từ 48f xuống 24f để luồng sáng sắc nét, gọn gàng hơn, không bị loang quá rộng
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

        // CHỈNH SỬA TỶ LỆ ĐỂ LUỒNG SÁNG GOM NHỎ LẠI:
        // Tăng từ 6% lên 12% để thanh LED ngắn lại và luồng sáng gom vào giữa hơn
        val topInset = w * 0.12f
        val topLeft = topInset
        val topRight = w - topInset

        // Đáy hình thang không cho tràn 100% nữa, thu hẹp lại cách 2 bên mép 4%
        val bottomInset = w * 0.04f
        val bottomLeft = bottomInset
        val bottomRight = w - bottomInset

        // Chiều dài luồng sáng (độ phủ dọc): lấy bằng chiều cao view
        val bottom = h.toFloat()

        trapPath.reset()
        trapPath.moveTo(topLeft, lineY)
        trapPath.lineTo(topRight, lineY)
        trapPath.lineTo(bottomRight, bottom)
        trapPath.lineTo(bottomLeft, bottom)
        trapPath.close()

        // Tinh chỉnh Gradient để ánh sáng gom ở trên và tan biến nhanh hơn khi xuống dưới
        glowPaint.shader = LinearGradient(
            w / 2f, lineY, w / 2f, bottom,
            intArrayOf(
                Color.argb(140, 255, 193, 7),   // Giảm nhẹ độ đậm đỉnh (từ 166 -> 140)
                Color.argb(45,  255, 193, 7),   // Làm mờ nhanh hơn ở giữa
                Color.argb(0,   255, 193, 7)    // Biến mất hoàn toàn ở đáy
            ),
            floatArrayOf(0f, 0.35f, 1f), // Đẩy điểm thu hẹp dải sáng lên 0.35f thay vì 0.5f
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawPath(trapPath, glowPaint)

        val lineY = 8f
        // Đồng bộ lại độ dài thanh LED trùng với đỉnh luồng sáng mới
        val topInset = width * 0.12f
        canvas.drawLine(topInset, lineY, width - topInset, lineY, linePaint)
        canvas.drawLine(topInset, lineY, width - topInset, lineY, solidLinePaint)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
}