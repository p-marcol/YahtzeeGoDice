// DiceManager.kt
// Singleton manager for GoDice devices
// Author: Piotr Marcol

package com.example.godicetest.managers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.models.Dice
import org.sample.godicesdklib.GoDiceSDK

/**
 * Singleton manager for GoDice devices.
 *
 * This class handles scanning, connecting, and managing multiple GoDice devices.
 * It listens for events from the GoDice SDK and notifies registered listeners about state changes.
 */
class DiceManager() : GoDiceSDK.Listener, IDiceManager {

    // region Singleton

    /**
     * Retrieves the singleton instance of the DiceManager.
     *
     * @return The DiceManager instance.
     */
    companion object {
        @Volatile
        private var INSTANCE: DiceManager? = null

        fun getInstance(): DiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiceManager().also { INSTANCE = it }
            }
        }
    }

    // endregion
    // region Properties

    private var bluetoothScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val dices = mutableMapOf<String, Dice>() // Address -> Dice
    private val diceIds = mutableListOf<String>() // List of Dice addresses

    private val listeners = mutableListOf<IDiceStateListener>()

    // endregion
    // region Initialization

    /**
     * Initializes the GoDice SDK listener.
     *
     * This must be done **before any SDK calls** to ensure all dice events are received properly.
     * If the listener is not set early enough, events (like color or roll updates) may be lost.
     *
     * All events are logged under the `DiceManager` tag for debugging.
     */
    init {
        GoDiceSDK.listener = object : GoDiceSDK.Listener {
            override fun onDiceColor(diceId: Int, color: Int) {
                Log.d("DiceManager", "Dice color changed: ID=$diceId, Color=$color")
                val dice = dices.values.firstOrNull { it.getSdkId() == diceId } ?: return
                dice.color.value = color
                listeners.forEach { it.onColorChanged(dice, color) }
            }

            override fun onDiceStable(diceId: Int, number: Int) {
                Log.d("DiceManager", "Dice stable: ID=$diceId, Face=$number")
                val dice = dices.values.firstOrNull { it.getSdkId() == diceId } ?: return
                dice.isStable.value = true
                dice.lastRoll.value = number
                listeners.forEach { it.onStable(dice, number) }
            }

            override fun onDiceRoll(diceId: Int, number: Int) {
                Log.d("DiceManager", "Dice rolling: ID=$diceId")
                val dice = dices.values.firstOrNull { it.getSdkId() == diceId } ?: return
                dice.isStable.value = false
                listeners.forEach { it.onRolling(dice) }
            }

            override fun onDiceChargingStateChanged(diceId: Int, charging: Boolean) {
                Log.d("DiceManager", "Dice charging state changed: ID=$diceId, Charging=$charging")
                val dice = dices.values.firstOrNull { it.getSdkId() == diceId } ?: return
                dice.isCharging.value = charging
                listeners.forEach { it.onChargingChanged(dice, charging) }
            }

            override fun onDiceChargeLevel(diceId: Int, level: Int) {
                Log.d("DiceManager", "Dice charge level: ID=$diceId, Level=$level")
                val dice = dices.values.firstOrNull { it.getSdkId() == diceId } ?: return
                dice.batteryLevel.value = level
                listeners.forEach { it.onChargeLevel(dice, level) }
            }
        }
    }

    // endregion
    // region Public Methods

    /**
     * Registers a listener to receive dice state updates.
     *
     * @param listener The listener to register.
     */
    override fun addListener(listener: IDiceStateListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: IDiceStateListener) {
        listeners.remove(listener)
    }

    /**
     * Starts scanning for GoDice devices using the provided Bluetooth adapter.
     *
     * @param adapter The Bluetooth adapter to use for scanning.
     * @param onComplete A callback function to be invoked once scanning has started.
     */
    @SuppressLint("MissingPermission")
    override fun startScan(adapter: BluetoothAdapter, onComplete: () -> Unit) {
        bluetoothScanner = adapter.bluetoothLeScanner

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(Dice.serviceUUID)).build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { handleScanResult(it) }
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                results?.forEach { it?.let { handleScanResult(it) } }
            }
        }

        bluetoothScanner?.startScan(filters, settings, scanCallback)
        onComplete()
    }

    /**
     * Connects to the specified Dice device.
     */
    @SuppressLint("MissingPermission")
    override fun connectDice(context: Context, dice: IDice) {
        val realDice = dice as? Dice ?: run {
            Log.w("DiceManager", "Unsupported dice implementation: ${dice::class.java.simpleName}")
            return
        }
        if (realDice.isConnected()) return // already connected
        val gatt = realDice.device.connectGatt(
            context,
            true,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?, status: Int, newState: Int
                ) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        realDice.bindGatt(gatt)
//                        runOnMainThread { listeners.forEach { it.onStable(realDice, 0) } }
                        realDice.onConnected()
                        runOnMainThread {
                            listeners.forEach { it.onConnectionChanged(realDice, true) }
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        realDice.closeGatt()
                        runOnMainThread {
                            listeners.forEach { it.onConnectionChanged(realDice, false) }
                            listeners.forEach { it.onDisconnected(realDice) }
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    realDice.onServicesDiscovered()
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
                ) {
                    realDice.onEvent()
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int
                ) {
                    realDice.nextWrite()
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int
                ) {
                    realDice.nextWrite()
                }
            }
        )
        realDice.bindGatt(gatt)
    }

    /**
     * Retrieves a Dice device by its Bluetooth address.
     *
     * @param address The Bluetooth address of the Dice device.
     * @return The Dice device if found, or null otherwise.
     */
    override fun getDice(address: String): Dice? = dices[address]

    /**
     * Retrieves a list of all discovered Dice devices.
     *
     * @return A list of all Dice devices.
     */
    override fun getAllDice(): List<Dice> = dices.values.toList()

    /**
     * Retrieves a list of Dice devices by their color.
     *
     * @param color The color value to filter Dice devices, as defined by GoDiceSDK.
     * @return A list of Dice devices matching the specified color.
     */
    override fun getDiceByColor(color: Int): List<Dice> {
        return dices.values.filter { it.color.value == color }
    }

    override fun turnOffAllDiceLed() {
        dices.values.forEach { dice ->
            dice.scheduleWrite(GoDiceSDK.closeToggleLedsPacket())
        }
    }

    override fun isConnected(dice: IDice): Boolean = dice.isConnected()

    // endregion
    // region Private Methods

    /**
     * Handles a scan result from the Bluetooth LE scanner.
     *
     * @param result The scan result to handle.
     */
    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val address = result.device.address
        val name = result.device.name ?: return
        if (!name.contains("GoDice") || dices.containsKey(address)) return

        var diceId = diceIds.indexOf(address)
        if (diceId < 0) {
            diceId = diceIds.size
            diceIds.add(address)
        }

        val dice = Dice(diceId, result.device)
        dices[address] = dice

        Log.d("DiceManager", "Discovered Dice: Name=$name, Address=$address")
        onNewDiceDetected()

        dice.blinkLed(0xff0000, 0.2f, 0.2f, 2)
        dice.scheduleWrite(GoDiceSDK.getColorPacket())
        dice.scheduleWrite(GoDiceSDK.getChargeLevelPacket())

        dice.bindGatt(null) // reset GATT to allow reconnection
    }

    /** Runs the provided runnable on the main UI thread.
     *
     * @param runnable The function to run on the main thread.
     */
    private fun runOnMainThread(runnable: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) runnable()
        else Handler(Looper.getMainLooper()).post { runnable() }
    }

    /**
     * Notifies listeners that a new Dice device has been detected.
     */
    private fun onNewDiceDetected() {
        Log.d("DiceManager", "New Dice detected")
        listeners.forEach { it.onNewDiceDetected() }
    }

    // endregion
    // region GoDiceSDK.Listener implementations

    /**
     * Called when a Dice returns its color value.
     *
     * @param diceId The SDK ID of the Dice device.
     * @param color The color value as an integer defined by GoDiceSDK.
     */
    override fun onDiceColor(diceId: Int, color: Int) {
        val dice = dices[diceIds[diceId]] ?: return
        dice.color.value = color
        Log.d("DiceManager", "Dice color: ID=$diceId, Color=$color")
        listeners.forEach { it.onColorChanged(dice, color) }
    }

    /**
     * Called when a Dice becomes stable and shows a face value.
     *
     * @param diceId The SDK ID of the Dice device.
     * @param face The face value shown on the Dice.
     */
    override fun onDiceStable(diceId: Int, face: Int) {
        val dice = dices[diceIds[diceId]] ?: return
        Log.d("DiceManager", "Dice stable: ID=$diceId, Face=$face")
        listeners.forEach { it.onStable(dice, face) }
    }

    /**
     * Called when a Dice is rolled.
     *
     * @param diceId The SDK ID of the Dice device.
     * @param number The number rolled (not used here).
     */
    override fun onDiceRoll(diceId: Int, number: Int) {
        val dice = dices[diceIds[diceId]] ?: return
        Log.d("DiceManager", "Dice rolling: ID=$diceId")
        listeners.forEach { it.onRolling(dice) }
    }

    /**
     * Called when a Dice's charging state changes.
     *
     * @param diceId The SDK ID of the Dice device.
     * @param charging True if the Dice is charging, false otherwise.
     */
    override fun onDiceChargingStateChanged(diceId: Int, charging: Boolean) {
        val dice = dices[diceIds[diceId]] ?: return
        Log.d("DiceManager", "Dice charging state changed: ID=$diceId, Charging=$charging")
        listeners.forEach { it.onChargingChanged(dice, charging) }
    }

    /**
     * Called when a Dice reports its charge level.
     *
     * @param diceId The SDK ID of the Dice device.
     * @param level The charge level as an integer percentage (0-100).
     */
    override fun onDiceChargeLevel(diceId: Int, level: Int) {
        val dice = dices[diceIds[diceId]] ?: return
        Log.d("DiceManager", "Dice charge level: ID=$diceId, Level=$level")
        listeners.forEach { it.onChargeLevel(dice, level) }
    }

    // endregion
}

// DiceManager.kt closed.
// Manager lights a cigarette, watches the dice roll on their own.
