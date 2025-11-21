package com.example.godicetest.managers

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.models.MockDice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * A mock implementation of the IDiceManager interface for testing purposes.
 * Behaves like a dice manager but uses mock dice instead of real ones and no Bluetooth functionality.
 * @constructor Creates a singleton instance of MockDiceManager.
 */
class MockDiceManager private constructor() : IDiceManager {

    companion object {
        @Volatile
        private var INSTANCE: MockDiceManager? = null

        fun getInstance(): MockDiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MockDiceManager().also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val listeners = mutableSetOf<IDiceStateListener>()
    private val diceList = mutableListOf<MockDice>()

    private var initialized = false

    private fun initialize(count: Int = 6) {
        if (initialized) return

        initialized = true
        repeat(count) { index ->
            val mockDice = MockDice(index).apply {
                mockAddress = "MOCK:$index"
            }
            diceList.add(mockDice)
        }
    }

    override fun addListener(listener: IDiceStateListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: IDiceStateListener) {
        listeners.remove(listener)
    }

    override fun startScan(
        adapter: BluetoothAdapter,
        onComplete: () -> Unit
    ) {
        if (!initialized) {
            initialize()
            diceList.forEach { _ ->
                listeners.forEach { it.onNewDiceDetected() }
            }
        }
        onComplete()
    }

    override fun connectDice(
        context: Context,
        dice: IDice
    ) {
        val found = diceList.find { it.id == dice.id } ?: return
        found.connect()
        listeners.forEach { it.onConnectionChanged(found, true) }
    }

    override fun getDice(address: String): IDice? {
        return diceList.find { it.mockAddress == address }
    }

    override fun getAllDice(): List<IDice> = diceList

    override fun getDiceByColor(color: Int): List<IDice> {
        return diceList.filter { it.color.value == color }
    }

    override fun turnOffAllDiceLed() {
        diceList.forEach { it.setLed(false) }
    }

    override fun isConnected(dice: IDice): Boolean = dice.isConnected()
}
