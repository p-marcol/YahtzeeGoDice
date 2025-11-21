package com.example.godicetest.managers

import com.example.godicetest.interfaces.IDiceManager

object DiceManagerFactory {
    enum class Mode {
        REAL,
        MOCK
    }

    var mode: Mode = Mode.REAL

    fun getManager(): IDiceManager {
        return when (mode) {
            Mode.REAL -> DiceManager.getInstance()
            Mode.MOCK -> MockDiceManager.getInstance()
        }
    }
}