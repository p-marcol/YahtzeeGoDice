package com.example.godicetest.enums

import android.content.Context
import androidx.annotation.StringRes
import com.example.godicetest.R
import com.example.godicetest.models.Dice

/**
 * Enum representing the different Yahtzee combinations.
 *
 * @property displayNameRes Resource ID for the display name of the combination.
 */
enum class eYahtzeeCombination(@StringRes val displayNameRes: Int) {
    //region Combination names
    ONES(R.string.ones),
    TWOS(R.string.twos),
    THREES(R.string.threes),
    FOURS(R.string.fours),
    FIVES(R.string.fives),
    SIXES(R.string.sixes),
    THREE_OF_A_KIND(R.string.three_of_a_kind),
    FOUR_OF_A_KIND(R.string.four_of_a_kind),
    FULL_HOUSE(R.string.full_House),
    SMALL_STRAIGHT(R.string.small_Straight),
    LARGE_STRAIGHT(R.string.large_Straight),
    YAHTZEE(R.string.yahtzee),
    CHANCE(R.string.chance);

    //endregion
    //region Public Methods

    /**
     * Gets the display name of the combination.
     *
     * @param context The context to access resources.
     * @return The display name string.
     */
    fun getDisplayName(context: Context): String = context.getString(displayNameRes)

    /**
     * Calculates the score for the combination based on the given dice.
     *
     * @param dice The list of dice to evaluate.
     * @return The calculated score.
     */
    fun score(dice: List<Dice>): Int {
        val values = dice.mapNotNull { it.lastRoll.value }
        if (values.size < 5) return 0

        val counts = values.groupingBy { it }.eachCount()

        return when (this) {
            ONES -> values.count { it == 1 } * 1
            TWOS -> values.count { it == 2 } * 2
            THREES -> values.count { it == 3 } * 3
            FOURS -> values.count { it == 4 } * 4
            FIVES -> values.count { it == 5 } * 5
            SIXES -> values.count { it == 6 } * 6
            THREE_OF_A_KIND -> if (counts.any { it.value >= 3 }) values.sum() else 0
            FOUR_OF_A_KIND -> if (counts.any { it.value >= 4 }) values.sum() else 0
            FULL_HOUSE -> {
                val freq = counts.values.sorted()
                if (freq == listOf(2, 3)) 25 else 0
            }

            SMALL_STRAIGHT -> if (hasStraight(values, 4)) 30 else 0
            LARGE_STRAIGHT -> if (hasStraight(values, 5)) 40 else 0
            YAHTZEE -> if (values.distinct().size == 1) 50 else 0
            CHANCE -> values.sum()
        }
    }

    /**
     * Gets the dice involved in the combination.
     *
     * @param dice The list of dice to evaluate.
     * @return The list of dice involved in the combination.
     */
    fun involvedDice(dice: List<Dice>): List<Dice> {
        val values = dice.mapNotNull { it.lastRoll.value }
        val counts = values.groupingBy { it }.eachCount()

        return when (this) {
            THREE_OF_A_KIND -> {
                val key = counts.entries.find { it.value == 3 }?.key
                dice.filter { it.lastRoll.value == key }.take(3)
            }

            FOUR_OF_A_KIND -> {
                val key = counts.entries.find { it.value == 4 }?.key
                dice.filter { it.lastRoll.value == key }.take(4)
            }

            FULL_HOUSE -> {
                val triple = counts.entries.find { it.value == 3 }?.key
                val pair = counts.entries.find { it.value == 2 }?.key
                dice.filter { it.lastRoll.value == triple || it.lastRoll.value == pair }
            }

            SMALL_STRAIGHT, LARGE_STRAIGHT -> {
                val straight = getStraightValues(values, if (this == SMALL_STRAIGHT) 4 else 5)
                dice.filter { it.lastRoll.value in straight }
            }

            YAHTZEE -> dice.takeIf { values.distinct().size == 1 } ?: emptyList()

            else -> emptyList()
        }
    }

    //endregion
    //region Private Methods

    /**
     * Checks if the dice contain a straight of the specified length.
     *
     * @param dice The list of dice values.
     * @param length The length of the straight to check for.
     * @return True if a straight of the specified length exists, false otherwise.
     */
    private fun hasStraight(dice: List<Int>, length: Int): Boolean =
        dice.toSet().sorted().windowed(length, 1).any { seq ->
            seq.zipWithNext().all { (a, b) -> b == a + 1 }
        }

    /**
     * Gets the straight values from the dice values.
     *
     * @param values The list of dice values.
     * @param length The length of the straight to retrieve.
     * @return The list of straight values if found, otherwise an empty list.
     */
    private fun getStraightValues(values: List<Int>, length: Int): List<Int> {
        val uniq = values.distinct().sorted()
        val straights = listOf(
            listOf(1, 2, 3, 4, 5),
            listOf(2, 3, 4, 5, 6)
        )
        val straight = straights.find { uniq.containsAll(it.take(length)) }
        return straight ?: emptyList()
    }
}

// End of eYahtzeeCombination.kt.
// Full house or Yahtzee… the bartender already winked at the devil.
