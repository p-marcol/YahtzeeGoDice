package com.example.godicetest.interfaces

import android.bluetooth.BluetoothAdapter
import android.content.Context

interface IDiceManager {
    fun addListener(listener: IDiceStateListener)
    fun removeListener(listener: IDiceStateListener)

    fun startScan(adapter: BluetoothAdapter, onComplete: () -> Unit)
    fun connectDice(context: Context, dice: IDice)

    fun isConnected(dice: IDice): Boolean

    fun getDice(address: String): IDice?
    fun getAllDice(): List<IDice>
    fun getDiceByColor(color: Int): List<IDice>

    fun turnOffAllDiceLed()
}
