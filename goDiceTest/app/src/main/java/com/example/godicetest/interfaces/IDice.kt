package com.example.godicetest.interfaces

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface representing a dice with rolling capabilities.
 */
interface IDice {
    //region Properties
    val id: Int
    val lastRoll: MutableStateFlow<Int?>
    val isStable: MutableStateFlow<Boolean?>
    val color: MutableStateFlow<Int?>
    val batteryLevel: MutableStateFlow<Int>
    val isCharging: MutableStateFlow<Boolean>

    val device: BluetoothDevice
    //endregion

    //region Identity
    fun getDieName(): String?
    //endregion

    //region Actions
    fun roll()

    fun setLed(on: Boolean)
    fun setLed(color1: Int, color2: Int)
    fun blinkLed(
        color: Int,
        onDuration: Float = .5f,
        offDuration: Float = .5f,
        blinks: Int = 2
    )
    //endregion

    //region Dice info
    fun getDicePattern(): List<Boolean>

    fun getColorName(): String?

    fun isConnected(): Boolean
    //endregion

    //region Connection lifecycle
    fun onConnected()

    fun onServicesDiscovered()

    fun onEvent()

    fun nextWrite()
    //endregion
}

// IDice.kt complete.
// Every die knows the rules… but the devil decides how they roll.
