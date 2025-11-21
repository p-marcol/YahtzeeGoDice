package com.example.godicetest.enums

/**
 * Enumeration representing the dot patterns for dice faces from 0 to 6.
 *
 * Each pattern is represented as a list of booleans, where `true` indicates
 * the presence of a dot and `false` indicates its absence. The list is structured
 * in a 3x3 grid format:
 * ```
 * [0] [1] [2]
 * [3] [4] [5]
 * [6] [7] [8]
 * ```
 */
enum class eDicePattern(val pattern: List<Boolean>) {
    //region Patterns
    Dice_0(
        listOf(
            false, false, false,
            false, false, false,
            false, false, false
        )
    ),
    Dice_1(
        listOf(
            false, false, false,
            false, true, false,
            false, false, false
        )
    ),
    Dice_2(
        listOf(
            true, false, false,
            false, false, false,
            false, false, true
        )
    ),
    Dice_3(
        listOf(
            true, false, false,
            false, true, false,
            false, false, true
        )
    ),
    Dice_4(
        listOf(
            true, false, true,
            false, false, false,
            true, false, true
        )
    ),
    Dice_5(
        listOf(
            true, false, true,
            false, true, false,
            true, false, true
        )
    ),
    Dice_6(
        listOf(
            true, false, true,
            true, false, true,
            true, false, true
        )
    ),
    //endregion
}

// End of eDicePattern.kt.
// Devil checks the dots, laughs, and lights a cigarette.
