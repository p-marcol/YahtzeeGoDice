package com.example.godicetest.managers

import android.util.Log
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener

/**
 * Manages the selection of a specific number of dice from the DiceManager.
 *
 * @param manager The DiceManager instance to listen to.
 * @param requiredCount The number of dice to select.
 * @param onSelectionConfirmed Callback invoked when the required number of dice have been selected.
 */
class DiceSelector(
    private val manager: IDiceManager,
    private val requiredCount: Int,
    private val onSelectionConfirmed: (List<IDice>) -> Unit
) : IDiceStateListener {

    //region Properties

    private val selectedDice = mutableSetOf<IDice>()
    private var ready = false

    //endregion
    //region Initialization

    init {
        manager.addListener(this)
        Log.d("Selection", "Selection button clicked with count: ${requiredCount}")
    }

    //endregion
    //region Public Methods

    /**
     * Confirms the selection of dice if user is ready.
     */
    fun confirmSelection() {
        if (ready) {
            manager.removeListener(this)
            onSelectionConfirmed(selectedDice.toList())
        }
    }

    //endregion
    //region Private Methods

    /**
     * Signals that the selection is ready by blinking the LEDs of the selected dice.
     */
    private fun signalReady() {
        selectedDice.forEach { dice ->
            dice.blinkLed(0x00ff00) // Green for ready
            // TODO: Trigger UI callback to inform user that selection is complete
        }
    }

    //endregion
    //region IDiceStateListener implementation

    /**
     * Called when a die becomes stable.
     *
     * @param dice The die that became stable.
     * @param face The face value of the die.
     */
    override fun onStable(dice: IDice, face: Int) {
        if (ready) return

        if (!selectedDice.contains(dice)) {
            selectedDice.add(dice)
        }

        if (selectedDice.size == requiredCount) {
            ready = true
            signalReady()
            // Stop listening to further state changes
            manager.removeListener(this)
            Log.d(
                "Selection",
                "Dice selected ($requiredCount): ${selectedDice.map { it.getColorName() }}"
            )
        }
    }

    override fun onRolling(dice: IDice) {}

    override fun onColorChanged(dice: IDice, color: Int) {}

    override fun onChargingChanged(dice: IDice, charging: Boolean) {}

    override fun onChargeLevel(dice: IDice, level: Int) {}

    override fun onDisconnected(dice: IDice) {}

    override fun onNewDiceDetected() {}

    override fun onConnectionChanged(dice: IDice, connected: Boolean) {}
    //endregion
}
