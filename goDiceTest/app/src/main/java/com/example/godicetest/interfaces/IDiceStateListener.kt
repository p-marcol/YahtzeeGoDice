package com.example.godicetest.interfaces

/**
 * Interface for listening to state changes in a Dice object.
 */
interface IDiceStateListener {
    //region Callbacks
    fun onColorChanged(dice: IDice, color: Int)
    fun onStable(dice: IDice, face: Int)
    fun onRolling(dice: IDice)
    fun onChargingChanged(dice: IDice, charging: Boolean)
    fun onChargeLevel(dice: IDice, level: Int)
    fun onDisconnected(dice: IDice)
    fun onNewDiceDetected()
    fun onConnectionChanged(dice: IDice, connected: Boolean)
    fun onLog(msg: String) {}
    //endregion
}

// IDiceStateListener.kt finished.
// Barman notes every flicker, but don’t worry… he’s on the house payroll.
