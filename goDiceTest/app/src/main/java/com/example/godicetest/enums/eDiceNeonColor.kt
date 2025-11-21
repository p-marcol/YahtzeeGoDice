package com.example.godicetest.enums

import androidx.core.graphics.toColorInt

/**
 * Enumeration representing neon colors for dice.
 *
 * Each color is associated with its hexadecimal color code.
 */
enum class eDiceNeonColor(
    val hexCode: String,
    val intColor: Int = hexCode.toColorInt()
) {
    //region Entries
    Black("#FFFFFF"),
    Red("#FF0100"),
    Green("#00FF0D"),
    Blue("#0009FF"),
    Yellow("#FFE300"),
    Orange("#FF5C00"),
    Unknown("#808080")
    //endregion
}

// End of eDiceNeonColor.kt.
// Shades ready, dice ready, souls optional.
