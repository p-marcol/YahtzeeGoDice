// Dice.kt
// Class representing a GoDice die and managing its Bluetooth GATT connection.
// Author: Piotr Marcol
package com.example.godicetest.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import com.example.godicetest.enums.eDicePattern
import kotlinx.coroutines.flow.MutableStateFlow
import org.sample.godicesdklib.GoDiceSDK
import java.util.LinkedList
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

/**
 * Represents a GoDice die, managing its Bluetooth GATT connection and state.
 * @param id The SDK ID of the die.
 * @param device The BluetoothDevice representing the die.
 */
@SuppressLint("MissingPermission")
class Dice(private val id: Int, val device: BluetoothDevice) {
    // region Bluetooth GATT variables
    var gatt: BluetoothGatt? = null
    private var service: BluetoothGattService? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var readChar: BluetoothGattCharacteristic? = null
    private var writes: Queue<ByteArray> = LinkedList()

    // endregion
    // region properties
    private var writeInProgress = false
    private var dieName = device.name
    var lastRoll = MutableStateFlow<Int?>(null)
    var isStable = MutableStateFlow<Boolean?>(true)
    var color = MutableStateFlow<Int?>(null)
    var batteryLevel = MutableStateFlow<Int>(0)
    var isCharging = MutableStateFlow<Boolean>(false)

    // endregion
    // region getters and setters

    /**
     * Returns the name of the die, or null if not available.
     */
    fun getDieName(): String? = dieName

    /**
     * Returns the SDK ID of the die.
     */
    fun getSdkId(): Int = id

    /**
     * Sets the LED state of the die.
     * @param on True to turn on the LED, false to turn it off.
     */
    fun setLed(on: Boolean) {
        if (on) {
            scheduleWrite(GoDiceSDK.openLedsPacket(0x0000ff, 0xffff00))
        } else {
            scheduleWrite(GoDiceSDK.closeToggleLedsPacket())
        }
    }

    /**
     * Sets the LED colors of the die.
     * @param color1 The first color in RGB format, ex. 0xff0000 for red.
     * @param color2 The second color in RGB format, ex. 0x00ff00 for green.
     */
    fun setLed(color1: Int, color2: Int) {
        scheduleWrite(GoDiceSDK.openLedsPacket(color1, color2))
    }

    fun getColorName() = when (color.value) {
        GoDiceSDK.DICE_BLACK -> "Black"
        GoDiceSDK.DICE_RED -> "Red"
        GoDiceSDK.DICE_GREEN -> "Green"
        GoDiceSDK.DICE_BLUE -> "Blue"
        GoDiceSDK.DICE_YELLOW -> "Yellow"
        GoDiceSDK.DICE_ORANGE -> "Orange"
        else -> "Unknown"
    }

    fun getDicePattern(): List<Boolean> {
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

    fun isConnected(): Boolean {
        return gatt != null
    }

    // endregion
    // region Bluetooth GATT handling

    /**
     * Called when the die is connected.
     */
    fun onConnected() {
        gatt?.discoverServices()
        Log.d("Dice", "$id: Connected to ${device.address}")
        // Start initialization packets after a short delay to ensure services are discovered
        Timer().schedule(object : TimerTask() {
            override fun run() {
                startInitPackets()
            }
        }, 1000)
    }

    /**
     * Called when services are discovered. It has to be set up before any read/write operations.
     */
    fun onServicesDiscovered() {
        service = gatt?.services?.firstOrNull { it.uuid == serviceUUID }
        writeChar = service?.characteristics?.firstOrNull { it.uuid == writeCharUUID }
        readChar = service?.characteristics?.firstOrNull { it.uuid == readCharUUID }
        readChar?.let {
            gatt?.setCharacteristicNotification(it, true)
            val descriptor = it.getDescriptor(CCCDUUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
        }
    }

    // endregion
    // region Data handling

    /**
     * Starts sending initialization packets to the die. Fetches the color and sends the initialization packet.
     */
    private fun startInitPackets() {
        scheduleWrite(GoDiceSDK.getColorPacket()) // Request color first
        scheduleWrite(
            GoDiceSDK.initializationPacket(
                GoDiceSDK.DICE_SENSITIVITY_DEFAULT,
                2,
                0.25f,
                0.25f,
                0x00ff00,
                GoDiceSDK.DiceBlinkMode.PARALLEL,
                GoDiceSDK.DiceLedsSelector.BOTH
            )
        ) // Initialization packet
    }

    /**
     * Called when a notification is received from the die, then processes the incoming data through the SDK.
     */
    fun onEvent() {
        readChar?.value?.let {
            Log.d("Dice", "$id: readChar value: ${it.joinToString(",")}")
            GoDiceSDK.incomingPacket(id, GoDiceSDK.DiceType.D6, it)
        }
    }

    /**
     * Processes the next write in the queue, if any.
     */
    fun nextWrite() {
        synchronized(writes) {
            writeInProgress = false
            writes.poll()?.let { value ->
                writeChar?.let { char ->
                    char.value = value
                    gatt?.writeCharacteristic(char)
                    writeInProgress = true
                }
            }
        }
    }

    /**
     * Schedules a write to the die by adding it to the queue and starting the write process if not already in progress.
     * @param value The byte array to write.
     */
    fun scheduleWrite(value: ByteArray) {
        synchronized(writes) {
            writes.add(value)
            if (!writeInProgress) {
                nextWrite()
            }
        }
    }

    fun blinkLed(color: String) {
        val colorInt = Integer.parseInt(color.removePrefix("#"), 16)
        scheduleWrite(GoDiceSDK.openLedsPacket(colorInt, colorInt))
        Timer().schedule(object : TimerTask() {
            override fun run() {
                scheduleWrite(GoDiceSDK.closeToggleLedsPacket())
            }
        }, 500)
    }

    // endregion
    // region UUIDs

    /** UUIDs for the GoDice service and characteristics */
    companion object {
        val serviceUUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val writeCharUUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val readCharUUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCDUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

// That’s all for Dice.kt.
// Don’t let the dice hear you pray.
