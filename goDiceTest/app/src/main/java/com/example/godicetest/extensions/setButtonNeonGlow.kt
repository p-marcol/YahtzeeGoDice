package com.example.godicetest.extensions

import android.content.res.Resources
import android.graphics.BlurMaskFilter
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import androidx.core.graphics.toColorInt

/**
 * Sets a neon glow effect on the Button.
 *
 * @param colorHex The color in hexadecimal format (e.g., "#FF00FF").
 * @param strokeWidth The width of the stroke around the Button.
 * @param cornerRadius The corner radius for rounded corners.
 * @param paddingDp The padding in dp to apply to the Button.
 * @param glowRadii The list of radii for the glow effect.
 */
fun Button.setNeonGlow(
    colorHex: String,
    strokeWidth: Int = 4,
    cornerRadius: Float = 24f,
    paddingDp: Int = 16,
    glowRadii: List<Float> = listOf(2f, 4f, 8f, 16f)
) {
    val color = colorHex.toColorInt()

    this.setTextColor(color)

    val scale = Resources.getSystem().displayMetrics.density
    val paddingPx = (paddingDp * scale + 0.5f).toInt()
    this.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

    val drawable = GradientDrawable()
    drawable.setColor(0x00000000) // Transparent background
    drawable.setStroke(strokeWidth, color)
    drawable.cornerRadius = cornerRadius
    this.background = drawable

    this.paint.isAntiAlias = true
    this.paint.maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL)


    glowRadii.forEach { radius ->
        this.setShadowLayer(
            radius,
            0f,
            0f,
            color
        )
    }
}

// setNeonGlow.kt finished.
// Your fingers are safe… but the neon might bite.
