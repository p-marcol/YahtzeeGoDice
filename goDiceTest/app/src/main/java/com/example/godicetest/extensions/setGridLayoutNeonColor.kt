package com.example.godicetest.extensions

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.godicetest.R

/**
 * Sets a neon glow effect on the GridLayout.
 *
 * @param colorHex The color in hexadecimal format (e.g., "#FF00FF").
 * @param strokeWidth The width of the stroke around the GridLayout.
 * @param glowRadii The list of radii for the glow effect.
 */
//region Neon glow
fun GridLayout.setNeonGlow(
    colorHex: String,
    strokeWidth: Int = 8,
    glowRadii: List<Float> = listOf(2f, 4f, 8f, 16f),
    strokeDrawable: Int = R.drawable.stroke_bg
) {
    val color = colorHex.toColorInt()

    val bg = ContextCompat.getDrawable(context, strokeDrawable)?.mutate()
    if (bg is GradientDrawable) {
        bg.setColor(0x00000000) // Transparent background
        bg.setStroke(strokeWidth, color)
        this.background = bg
    }

    this.backgroundTintList = ColorStateList.valueOf(color)
    this.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    this.background.alpha = 180
    this.elevation = 10f
    this.translationZ = 10f
}
//endregion

// GridLayout set. The devil rearranges the dots while you blink.
