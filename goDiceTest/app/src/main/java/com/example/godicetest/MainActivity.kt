// MainActivity.kt
// Main activity for the application.
// Author: Piotr Marcol
package com.example.godicetest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.activities.PlayerSetupActivity
import com.example.godicetest.adapters.DiceAdapter
import com.example.godicetest.adapters.DiceViewAdapter
import com.example.godicetest.extensions.setNeonColor
import com.example.godicetest.extensions.setNeonGlow
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.managers.DiceManagerFactory
import com.example.godicetest.managers.DiceSelector
import com.example.godicetest.utils.getDiceColorName
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Main activity for the GoDice test application.
 */
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    //region Properties
    private lateinit var diceManager: IDiceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var diceAdapter: DiceAdapter
    private lateinit var textView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var diceViewer: RecyclerView
    private lateinit var diceViewAdapter: DiceViewAdapter
    private var diceSelector: DiceSelector? = null
    private lateinit var languageButton: Button
    private var hasScannedDice: Boolean = false
    //endregion

    //region Lifecycle
    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBars()
        applySafeAreaInsets(findViewById(R.id.constraint))

        textView = findViewById(R.id.textView)
        scrollView = findViewById(R.id.scrollView)
        recyclerView = findViewById(R.id.diceRecyclerView)
        diceViewer = findViewById(R.id.diceViewer)
        languageButton = findViewById(R.id.languageButton)

        DiceManagerFactory.mode = if (BuildConfig.USE_MOCK_DICE) {
            DiceManagerFactory.Mode.MOCK
        } else {
            DiceManagerFactory.Mode.REAL
        }

        diceManager = DiceManagerFactory.getManager()
        diceManager.addListener(object : IDiceStateListener {
            override fun onColorChanged(dice: IDice, color: Int) {
                val colorName = getDiceColorName(color)
                Log.d(
                    "MainActivity",
                    "Dice ${dice.getDieName()} Color Changed: $colorName"
                )
                appendLog("Dice ${dice.getDieName()} Color Changed: $colorName")
            }

            override fun onStable(dice: IDice, face: Int) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Stable: $face")
                appendLog("Dice ${dice.getDieName()} Stable: $face")
            }

            override fun onRolling(dice: IDice) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Rolling")
                appendLog("Dice ${dice.getDieName()} Rolling")
            }

            override fun onChargingChanged(dice: IDice, charging: Boolean) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Charging: $charging")
                appendLog("Dice ${dice.getDieName()} Charging: $charging")
            }

            override fun onChargeLevel(dice: IDice, level: Int) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Battery Level: $level")
                appendLog("Dice ${dice.getDieName()} Battery Level: $level")
            }

            override fun onDisconnected(dice: IDice) {
                Log.d("MainActivity", "Dice ${dice.getDieName()} Disconnected")
                appendLog("Dice ${dice.getDieName()} Disconnected")
            }

            override fun onNewDiceDetected() {
                runOnUiThread { updateDiceList() }
            }

            override fun onConnectionChanged(
                dice: IDice,
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
            { dice, view -> showDicePopover(view, dice) },
            diceManager::isConnected
        )

        recyclerView.adapter = diceAdapter

        diceViewer.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        diceViewAdapter =
            DiceViewAdapter(
                diceManager.getAllDice().filter { dice -> diceManager.isConnected(dice) },
                onDiceClick = { dice, view -> dice.blinkLed(0x00FF00) }
            )

        diceViewer.adapter = diceViewAdapter

        val scanButton = findViewById<Button>(R.id.scanButton)
        val diceSelection = findViewById<LinearLayout>(R.id.diceSelection)
        val gotoGameBtn = findViewById<Button>(R.id.goto_game)
        scanButton.setNeonColor("#FF00FF")

        hasScannedDice = savedInstanceState?.getBoolean(KEY_HAS_SCANNED_DICE)
            ?: diceManager.getAllDice().isNotEmpty()
        applyScanUiState(scanButton, diceSelection, gotoGameBtn)

        scanButton.setOnClickListener {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter == null) {
                Toast.makeText(
                    this,
                    getString(R.string.bluetooth_required_for_scan),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            PermissionsHelper.requestPermissions(this, adapter) {
                if (!adapter.isEnabled) {
                    Toast.makeText(
                        this,
                        getString(R.string.bluetooth_required_for_scan),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@requestPermissions
                }
                if (!BuildConfig.USE_MOCK_DICE && !isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        getString(R.string.location_required_for_scan),
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    return@requestPermissions
                }
                scanButton.isEnabled = false
                scanButton.visibility = View.GONE
                diceSelection.visibility = View.VISIBLE
                gotoGameBtn.visibility = View.VISIBLE
                hasScannedDice = true
                diceManager.startScan(adapter) {}
            }
        }

        languageButton.setNeonGlow(
            "#00FFAA",
            strokeWidth = 2,
            cornerRadius = 14f,
            paddingDp = 8,
            glowRadii = listOf(2f, 4f, 8f)
        )
        updateLanguageButtonLabel()

        val selectButton = findViewById<Button>(R.id.selectionButton)

        selectButton.setNeonColor("#00FFFF")

        selectButton.setOnClickListener {
            startDiceSelection()
        }

        gotoGameBtn.setNeonGlow("#FFFF00")

        gotoGameBtn.setOnClickListener {
            val connectedDice = diceManager.getAllDice().filter { diceManager.isConnected(it) }
            val selectedDice = diceSelector?.getSelectedDice()
            Log.d(
                "Selection",
                "Connected dice: ${connectedDice.size}, Selected dice: ${selectedDice?.size ?: 0}"
            )
            val diceForGame = when {
                selectedDice != null && selectedDice.size == 5 -> selectedDice
                selectedDice.isNullOrEmpty() -> connectedDice
                else -> selectedDice
            }
            if (diceForGame.size != 5) {
                Toast.makeText(
                    this,
                    getString(R.string.exactly_five_dice_required),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            prepareDiceForGame(diceForGame)

            val intent = Intent(this, PlayerSetupActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        languageButton.setOnClickListener {
            // Android 13+ - systemowy picker
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }
    //endregion

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HAS_SCANNED_DICE, hasScannedDice)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        updateLanguageButtonLabel()
    }

    //region UI helpers
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
            onInfoClick = { dice, view -> showDicePopover(view, dice) },
            isDiceConnected = diceManager::isConnected
        )
        recyclerView.adapter = diceAdapter
        diceViewAdapter =
            DiceViewAdapter(
                diceManager.getAllDice().filter { dice -> diceManager.isConnected(dice) },
                onDiceClick = { dice, view -> dice.blinkLed(0x00FF00) }
            )
        diceViewer.adapter = diceViewAdapter
        Log.d("DiceList", "Dice list updated")
    }

    private fun applyScanUiState(
        scanButton: Button,
        diceSelection: LinearLayout,
        gotoGameBtn: Button
    ) {
        if (hasScannedDice) {
            scanButton.visibility = View.GONE
            scanButton.isEnabled = false
            diceSelection.visibility = View.VISIBLE
            gotoGameBtn.visibility = View.VISIBLE
        } else {
            scanButton.visibility = View.VISIBLE
            scanButton.isEnabled = true
            diceSelection.visibility = View.INVISIBLE
            gotoGameBtn.visibility = View.INVISIBLE
        }
    }
    //endregion

    private fun applySafeAreaInsets(root: View) {
        val initial = Insets.of(
            root.paddingLeft,
            root.paddingTop,
            root.paddingRight,
            root.paddingBottom
        )

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.displayCutout
            val cutoutLeft = cutout?.safeInsetLeft ?: 0
            val cutoutTop = cutout?.safeInsetTop ?: 0
            val cutoutRight = cutout?.safeInsetRight ?: 0
            val cutoutBottom = cutout?.safeInsetBottom ?: 0

            val left = max(systemBars.left, cutoutLeft)
            val top = max(systemBars.top, cutoutTop)
            val right = max(systemBars.right, cutoutRight)
            val bottom = max(systemBars.bottom, cutoutBottom)

            view.setPadding(
                initial.left + left,
                initial.top + top,
                initial.right + right,
                initial.bottom + bottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(root)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return false
        return locationManager.isLocationEnabled
    }

    private fun applySystemBars() {
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }


    //region Game setup hook
    /**
     * Placeholder to apply exactly 5 selected dice to the game manager before starting.
     * Replace the body with actual selection/assignment logic when ready.
     */
    private fun prepareDiceForGame(selectedDice: List<IDice>) {
        val dicesToUse = selectedDice
        val dicesToDisconnect =
            diceManager.getAllDice().filter { dice -> dice.id !in dicesToUse.map { it.id } }
        dicesToDisconnect.forEach { dice -> diceManager.disconnectDice(dice) }
    }
    //endregion

    //region Popovers
    /**
     * Shows a popover with dice information and controls.
     *
     * @param anchor The view to anchor the popover to.
     * @param dice The dice object to display information for.
     */
    private fun showDicePopover(anchor: View, dice: IDice) {
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

        tvName.text = dice.getDieName() ?: "Dice ${dice.id}"

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
    //endregion

    //region Language
    private fun updateLanguageButtonLabel() {
        val locale = resources.configuration.locales[0]
        val label = when (locale.language.lowercase()) {
            "pl" -> getString(R.string.language_short_pl)
            else -> getString(R.string.language_short_en)
        }
        languageButton.text = label
    }
    //endregion

    //region Logging
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
    //endregion

    private companion object {
        private const val KEY_HAS_SCANNED_DICE = "has_scanned_dice"
    }
}

// End of MainActivity.kt.
// Welcome to the Devil’s Dice Bar. First drink is always a trap.
