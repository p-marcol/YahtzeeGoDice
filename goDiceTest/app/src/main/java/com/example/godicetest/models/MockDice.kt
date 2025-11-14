package com.example.godicetest.models

import com.example.godicetest.interfaces.IDice
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A mock implementation of a dice for testing purposes.
 *
 * @param id The unique identifier for the mock dice.
 */
class MockDice(override val id: Int) : IDice {
    override val lastRoll = MutableStateFlow<Int?>(null)
    override val isStable = MutableStateFlow<Boolean?>(true)

    /**
     * Simulates rolling the mock dice.
     */
    override fun roll() {
        isStable.value = false
        val rolled = (1..6).random()
        println("Mock dice $id rolled: $rolled")
        lastRoll.value = rolled
        isStable.value = true
    }
}

// End of MockDice.kt.
// The bartender laughs. Even fake dice have stories to tell.
