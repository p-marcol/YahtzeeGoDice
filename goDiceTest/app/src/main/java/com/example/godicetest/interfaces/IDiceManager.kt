package com.example.godicetest.interfaces

import android.bluetooth.BluetoothAdapter
import android.content.Context

interface IDiceManager {
    //region Listener management
    fun addListener(listener: IDiceStateListener)
    fun removeListener(listener: IDiceStateListener)
    //endregion

    //region Connections
    fun startScan(adapter: BluetoothAdapter, onComplete: () -> Unit)
    fun connectDice(context: Context, dice: IDice)

    fun isConnected(dice: IDice): Boolean
    //endregion

    //region Dice lookup
    fun getDice(address: String): IDice?
    fun getAllDice(): List<IDice>
    fun getDiceByColor(color: Int): List<IDice>
    //endregion

    //region LED utilities
    fun turnOffAllDiceLed()
    //endregion
}
