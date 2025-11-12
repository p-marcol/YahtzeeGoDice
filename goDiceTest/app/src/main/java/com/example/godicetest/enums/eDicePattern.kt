package com.example.godicetest.enums

enum class eDicePattern(val pattern: List<Boolean>) {
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
}

// End of eDicePattern.kt.
// Devil checks the dots, laughs, and lights a cigarette.
