package com.example.godicetest.interfaces

import com.example.godicetest.models.Dice

interface IDiceStateListener {
    fun onColorChanged(dice: Dice, color: Int)
    fun onStable(dice: Dice, face: Int)
    fun onRolling(dice: Dice)
    fun onChargingChanged(dice: Dice, charging: Boolean)
    fun onChargeLevel(dice: Dice, level: Int)
    fun onDisconnected(dice: Dice)
    fun onNewDiceDetected()
    fun onConnectionChanged(dice: Dice, connected: Boolean)
    fun onLog(msg: String) {}
}

// IDiceStateListener.kt finished.
// Barman notes every flicker, but don’t worry… he’s on the house payroll.
