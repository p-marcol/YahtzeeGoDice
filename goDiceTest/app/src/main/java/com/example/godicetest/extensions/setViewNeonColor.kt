package com.example.godicetest.extensions

import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.toColorInt

fun View.setNeonColor(
    colorHex: String
) {
    val color = colorHex.toColorInt()
    val d: Drawable? = this.background?.mutate()
    if (d != null) {
        val wrapped = DrawableCompat.wrap(d)
        DrawableCompat.setTint(wrapped, color)
        this.background = wrapped
    }
}