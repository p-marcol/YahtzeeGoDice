package com.example.godicetest.enums

enum class eYahtzeeCombination {
    ONES {
        override fun score(dice: List<Int>): Int = dice.filter { it == 1 }.sum()
    },
    TWOS {
        override fun score(dice: List<Int>): Int = dice.filter { it == 2 }.sum()
    },
    THREES {
        override fun score(dice: List<Int>): Int = dice.filter { it == 3 }.sum()
    },
    FOURS {
        override fun score(dice: List<Int>): Int = dice.filter { it == 4 }.sum()
    },
    FIVES {
        override fun score(dice: List<Int>): Int = dice.filter { it == 5 }.sum()
    },
    SIXES {
        override fun score(dice: List<Int>): Int = dice.filter { it == 6 }.sum()
    },
    THREE_OF_A_KIND {
        override fun score(dice: List<Int>): Int {
            val counts = dice.groupingBy { it }.eachCount()
            return if (counts.values.any { it >= 3 }) dice.sum() else 0
        }
    },
    FOUR_OF_A_KIND {
        override fun score(dice: List<Int>): Int {
            val counts = dice.groupingBy { it }.eachCount()
            return if (counts.values.any { it >= 4 }) dice.sum() else 0
        }
    },
    FULL_HOUSE {
        override fun score(dice: List<Int>): Int {
            val counts = dice.groupingBy { it }.eachCount()
            return if (counts.values.containsAll(listOf(3, 2))) 25 else 0
        }
    },
    SMALL_STRAIGHT {
        override fun score(dice: List<Int>): Int =
            if (hasStraight(dice, 4)) 30 else 0
    },
    LARGE_STRAIGHT {
        override fun score(dice: List<Int>): Int =
            if (hasStraight(dice, 5)) 40 else 0
    },
    YAHTZEE {
        override fun score(dice: List<Int>): Int {
            val counts = dice.groupingBy { it }.eachCount()
            return if (counts.values.any { it == 5 }) 50 else 0
        }
    },
    CHANCE {
        override fun score(dice: List<Int>): Int = dice.sum()
    };

    abstract fun score(dice: List<Int>): Int

    companion object {
        fun hasStraight(dice: List<Int>, length: Int): Boolean =
            dice.toSet().sorted().windowed(length, 1).any { seq ->
                seq.zipWithNext().all { (a, b) -> b == a + 1 }
            }
    }
}

