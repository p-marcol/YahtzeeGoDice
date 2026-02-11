package com.example.godicetest.managers

import com.example.godicetest.interfaces.IDiceManager

object DiceManagerFactory {
    //region Mode configuration
    enum class Mode {
        REAL,
        MOCK
    }

    var mode: Mode = Mode.REAL
    //endregion

    //region Factory
    fun getManager(): IDiceManager {
        return when (mode) {
            Mode.REAL -> DiceManager.getInstance()
            Mode.MOCK -> MockDiceManager.getInstance()
        }
    }
    //endregion
}

// End of DiceManagerFactory.kt.
// Some nights are real. Some are rehearsals.

