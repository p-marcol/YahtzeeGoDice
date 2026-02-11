package com.example.godicetest.utils

import android.content.Context
import com.example.godicetest.R
import org.sample.godicesdklib.GoDiceSDK

fun Context.getDiceColorName(color: Int?): String {
    return when (color) {
        GoDiceSDK.DICE_BLACK -> getString(R.string.dice_color_black)
        GoDiceSDK.DICE_RED -> getString(R.string.dice_color_red)
        GoDiceSDK.DICE_GREEN -> getString(R.string.dice_color_green)
        GoDiceSDK.DICE_BLUE -> getString(R.string.dice_color_blue)
        GoDiceSDK.DICE_YELLOW -> getString(R.string.dice_color_yellow)
        GoDiceSDK.DICE_ORANGE -> getString(R.string.dice_color_orange)
        else -> getString(R.string.dice_color_unknown)
    }
}
