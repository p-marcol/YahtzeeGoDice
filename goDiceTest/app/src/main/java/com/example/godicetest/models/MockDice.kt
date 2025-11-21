package com.example.godicetest.models

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.godicetest.enums.eDicePattern
import com.example.godicetest.interfaces.IDice
import kotlinx.coroutines.flow.MutableStateFlow
import org.sample.godicesdklib.GoDiceSDK

/**
 * A mock implementation of a dice for testing purposes.
 *
 * @param id The unique identifier for the mock dice.
 */
class MockDice(override val id: Int) : IDice {
    override val lastRoll = MutableStateFlow<Int?>(null)
    override val isStable = MutableStateFlow<Boolean?>(true)
    override val color = MutableStateFlow<Int?>(GoDiceSDK.DICE_BLUE)
    override val batteryLevel = MutableStateFlow<Int>(100)
    override val isCharging = MutableStateFlow<Boolean>(false)

    override val device: BluetoothDevice
        get() = throw UnsupportedOperationException("Mock dice does not expose a BluetoothDevice")

    var mockAddress: String = "MOCK:$id"

    private var connected = false

    override fun getDieName(): String = "Mock $id"

    override fun isConnected(): Boolean = connected
    override fun onConnected() {}

    override fun onServicesDiscovered() {}

    override fun onEvent() {}

    override fun nextWrite() {}

    fun connect() {
        connected = true
        Log.d("MockDice", "Mock dice $id connected.")
    }

    fun disconnect() {
        connected = false
        Log.d("MockDice", "Mock dice $id disconnected.")
    }

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

    override fun setLed(on: Boolean) {}

    override fun setLed(color1: Int, color2: Int) {}

    override fun blinkLed(
        color: Int,
        onDuration: Float,
        offDuration: Float,
        blinks: Int
    ) {
    }

    override fun getDicePattern(): List<Boolean> {
        return when (lastRoll.value) {
            1 -> eDicePattern.Dice_1.pattern
            2 -> eDicePattern.Dice_2.pattern
            3 -> eDicePattern.Dice_3.pattern
            4 -> eDicePattern.Dice_4.pattern
            5 -> eDicePattern.Dice_5.pattern
            6 -> eDicePattern.Dice_6.pattern
            else -> eDicePattern.Dice_0.pattern
        }
    }

    override fun getColorName(): String {
        return when (color.value) {
            GoDiceSDK.DICE_BLACK -> "Black"
            GoDiceSDK.DICE_RED -> "Red"
            GoDiceSDK.DICE_GREEN -> "Green"
            GoDiceSDK.DICE_BLUE -> "Blue"
            GoDiceSDK.DICE_YELLOW -> "Yellow"
            else -> "Unknown"
        }
    }
}

// End of MockDice.kt.
// The bartender laughs. Even fake dice have stories to tell.
