// MainActivity.kt
// Main activity for the application.
// Author: Piotr Marcol
package com.example.godicetest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.adapters.DiceViewAdapter
import com.example.godicetest.extensions.setNeonColor
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.managers.DiceManager
import com.example.godicetest.managers.DiceSelector
import com.example.godicetest.models.Dice
import kotlinx.coroutines.launch

/**
 * Main activity for the GoDice test application.
 */
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private lateinit var diceManager: DiceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var diceAdapter: DiceAdapter
    private lateinit var textView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var diceViewer: RecyclerView
    private lateinit var diceViewAdapter: DiceViewAdapter
    private lateinit var diceSelector: DiceSelector

    /**
     * Shows a popover with dice information and controls.
     *
     * @param anchor The view to anchor the popover to.
     * @param dice The dice object to display information for.
     */
    private fun showDicePopover(anchor: View, dice: Dice) {
        val inflater = LayoutInflater.from(anchor.context)
        val popupView = inflater.inflate(R.layout.popover_dice, null)

        val popup = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.isOutsideTouchable = true
        popup.elevation = 10f

        val tvName = popupView.findViewById<TextView>(R.id.tvDiceName)
        val tvBattery = popupView.findViewById<TextView>(R.id.tvBattery)
        val btnLedOn = popupView.findViewById<Button>(R.id.btnLedOn)
        val btnLedOff = popupView.findViewById<Button>(R.id.btnLedOff)

        tvName.text = dice.getDieName() ?: "Dice ${dice.getSdkId()}"

        btnLedOn.setOnClickListener { dice.setLed(true) }
        btnLedOff.setOnClickListener { dice.setLed(false) }

        (anchor.context as? AppCompatActivity)?.lifecycleScope?.launch {
            launch {
                dice.batteryLevel.collect { level ->
                    val chargingText = if (dice.isCharging.value) " (Charging)" else ""
                    tvBattery.text = "Battery: $level%$chargingText"
                }
            }
        }

        // pokaż nad anchorem
        popup.showAsDropDown(anchor, 0, -anchor.height)
    }

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        scrollView = findViewById(R.id.scrollView)
        recyclerView = findViewById(R.id.diceRecyclerView)
        diceViewer = findViewById(R.id.diceViewer)

        diceManager = DiceManager.getInstance()
        diceManager.addListener(object : IDiceStateListener {
            override fun onColorChanged(dice: Dice, color: Int) {
                Log.d(
                    "MainActivity",
                    "Dice ${dice.getDieName()} Color Changed: ${dice.getColorName()}"
                )
                appendLog("Dice ${dice.getDieName()} Color Changed: ${dice.getColorName()}")
            }

            override fun onStable(dice: Dice, face: Int) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Stable: $face")
                appendLog("Dice ${dice.getDieName()} Stable: $face")
            }

            override fun onRolling(dice: Dice) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Rolling")
                appendLog("Dice ${dice.getDieName()} Rolling")
            }

            override fun onChargingChanged(dice: Dice, charging: Boolean) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Charging: $charging")
                appendLog("Dice ${dice.getDieName()} Charging: $charging")
            }

            override fun onChargeLevel(dice: Dice, level: Int) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Battery Level: $level")
                appendLog("Dice ${dice.getDieName()} Battery Level: $level")
            }

            override fun onDisconnected(dice: Dice) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Disconnected")
                appendLog("Dice ${dice.getDieName()} Disconnected")
            }

            override fun onNewDiceDetected() {
                runOnUiThread { updateDiceList() }
            }

            override fun onConnectionChanged(
                dice: Dice,
                connected: Boolean
            ) {
                Log.d(
                    "MainActivity",
                    "Dice ${dice.getDieName()} Connection Changed: $connected"
                )
                appendLog("Dice ${dice.getDieName()} Connection Changed: $connected")
                runOnUiThread { updateDiceList() }
            }

            override fun onLog(msg: String) {
                appendLog(msg)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        diceAdapter = DiceAdapter(
            diceManager.getAllDice(),
            { dice -> diceManager.connectDice(this, dice) },
            { dice, view -> showDicePopover(view, dice) }
        )

        recyclerView.adapter = diceAdapter

        diceViewer.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        diceViewAdapter =
            DiceViewAdapter(
                diceManager.getAllDice().filter { dice -> dice.isConnected() },
                onDiceClick = { dice, view -> dice.blinkLed(0x00FF00) }
            )

        diceViewer.adapter = diceViewAdapter

        val scanButton = findViewById<Button>(R.id.scanButton)
        val diceSelection = findViewById<LinearLayout>(R.id.diceSelection)
        scanButton.setNeonColor("#FF00FF")

        scanButton.setOnClickListener {
            it.isEnabled = false
            it.visibility = View.GONE
            diceSelection.visibility = View.VISIBLE
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            PermissionsHelper.requestPermissions(this, adapter) {
                diceManager.startScan(adapter) {}
            }
        }

        val selectButton = findViewById<Button>(R.id.selectionButton)

        selectButton.setNeonColor("#00FFFF")

        selectButton.setOnClickListener {
            startDiceSelection()
        }
    }

    /**
     * Starts the dice selection process.
     */
    private fun startDiceSelection() {
        val selectCount = findViewById<TextView>(R.id.selectionCount)
        val count = selectCount.text.toString().toIntOrNull() ?: 1
        appendLog("Starting dice selection for $count dice.")
        diceManager.turnOffAllDiceLed()
        diceSelector = DiceSelector(
            diceManager,
            count
        ) { selectedDice ->
            Log.d(
                "Selection",
                "Dice selection confirmed with ${selectedDice.size} dice."
            )
            appendLog("Dice selection confirmed with ${selectedDice.size} dice.")
        }
    }

    /**
     * Updates the dice list in the UI.
     */
    private fun updateDiceList() {
        diceAdapter = DiceAdapter(
            diceManager.getAllDice(),
            onConnectClick = { dice -> diceManager.connectDice(this, dice) },
            onInfoClick = { dice, view -> showDicePopover(view, dice) }
        )
        recyclerView.adapter = diceAdapter
        diceViewAdapter =
            DiceViewAdapter(
                diceManager.getAllDice().filter { dice -> dice.isConnected() },
                onDiceClick = { dice, view -> dice.blinkLed(0x00FF00) }
            )
        diceViewer.adapter = diceViewAdapter
        Log.d("DiceList", "Dice list updated")
    }

    /**
     * Appends a log message to the text view.
     *
     * @param msg The message to append.
     */
    private fun appendLog(msg: String) {
        runOnUiThread {
            textView.append("$msg\n")
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}

// End of MainActivity.kt.
// Welcome to the Devil’s Dice Bar. First drink is always a trap.
