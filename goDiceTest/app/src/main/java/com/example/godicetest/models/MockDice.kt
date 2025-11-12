package com.example.godicetest.models

import com.example.godicetest.interfaces.IDice
import kotlinx.coroutines.flow.MutableStateFlow

class MockDice(override val id: Int) : IDice {
    override val lastRoll = MutableStateFlow<Int?>(null)
    override val isStable = MutableStateFlow<Boolean?>(true)

    override fun roll() {
        isStable.value = false
        val rolled = (1..6).random()
        println("🎲 Mock dice $id rolled: $rolled")
        lastRoll.value = rolled
        isStable.value = true
    }
}

// End of MockDice.kt.
// The bartender laughs. Even fake dice have stories to tell.
