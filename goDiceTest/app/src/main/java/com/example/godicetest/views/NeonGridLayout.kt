package com.example.godicetest.views

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.GridLayout
import androidx.core.graphics.toColorInt

/**
 * A custom GridLayout with a neon glow effect.
 */
class NeonGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    //region Paint configuration
    /** Paint object for drawing the neon glow effect. */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.OUTER)
    }

    private val rect = RectF()

    private var cornerRadius = 24f
    //endregion

    //region Customization
    /** Sets the neon color of the grid layout.
     * @param colorHex The color in hexadecimal format (e.g., "#FF00FF").
     */
    fun setNeonColor(colorHex: String) {
        paint.color = colorHex.toColorInt()
        invalidate()
    }

    /** Sets the corner radius for the rounded corners.
     * @param radius The corner radius in pixels.
     */
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }
    //endregion

    //region Drawing
    /**
     * Draws the neon glow effect around the grid layout.
     *
     * @param canvas The canvas on which to draw.
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    //endregion
}

// NeonGridLayout.kt finished.
// The devil admires the glow while moving the dice behind your back.
