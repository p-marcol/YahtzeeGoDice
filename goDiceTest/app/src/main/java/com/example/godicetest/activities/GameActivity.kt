package com.example.godicetest.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.godicetest.R
import com.example.godicetest.enums.eYahtzeeCombination
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.managers.DiceManagerFactory
import com.example.godicetest.views.DiceSet
import org.sample.godicesdklib.GoDiceSDK
import kotlin.math.max

class GameActivity : AppCompatActivity() {

    private lateinit var diceManager: IDiceManager
    private lateinit var playerNameView: TextView
    private lateinit var rollInfoText: TextView
    private lateinit var turnDiceSet: DiceSet
    private val diceSetsByCombination = mutableMapOf<eYahtzeeCombination, DiceSet>()
    private val players = mutableListOf<PlayerState>()
    private var currentPlayerIndex = 0
    private val placeholderFaces = mapOf(
        eYahtzeeCombination.ONES to List(5) { 1 },
        eYahtzeeCombination.TWOS to List(5) { 2 },
        eYahtzeeCombination.THREES to List(5) { 3 },
        eYahtzeeCombination.FOURS to List(5) { 4 },
        eYahtzeeCombination.FIVES to List(5) { 5 },
        eYahtzeeCombination.SIXES to List(5) { 6 },
        eYahtzeeCombination.THREE_OF_A_KIND to listOf(3, 3, 3, 5, 6),
        eYahtzeeCombination.FOUR_OF_A_KIND to listOf(4, 4, 4, 4, 2),
        eYahtzeeCombination.FULL_HOUSE to listOf(5, 5, 5, 2, 2),
        eYahtzeeCombination.SMALL_STRAIGHT to listOf(1, 2, 3, 4, 6),
        eYahtzeeCombination.LARGE_STRAIGHT to listOf(2, 3, 4, 5, 6),
        eYahtzeeCombination.YAHTZEE to List(5) { 6 },
        eYahtzeeCombination.CHANCE to listOf(1, 3, 4, 5, 6)
    )

    private val requiredTurnDiceIds = mutableListOf<Int>()
    private val turnRollSnapshots = mutableMapOf<Int, DiceSnapshot>()
    private val uiHandler = Handler(Looper.getMainLooper())
    private var missingDiceToggleOn = false

    private val missingDiceToggleRunnable = object : Runnable {
        override fun run() {
            missingDiceToggleOn = !missingDiceToggleOn
            refreshOnUi()
            uiHandler.postDelayed(this, MISSING_DICE_TOGGLE_INTERVAL_MS)
        }
    }

    private val requiredDiceLedRunnable = object : Runnable {
        override fun run() {
            flashRequiredDiceLeds()
            uiHandler.postDelayed(this, REQUIRED_DICE_LED_FLASH_INTERVAL_MS)
        }
    }

    private val diceStateListener = object : IDiceStateListener {
        override fun onStable(dice: IDice, face: Int) {
            syncRequiredTurnDice()
            if (dice.id in requiredTurnDiceIds) {
                turnRollSnapshots[dice.id] = DiceSnapshot(face, dice.color.value)
            }
            refreshOnUi()
        }

        override fun onRolling(dice: IDice) = Unit

        override fun onColorChanged(dice: IDice, color: Int) {
            val existing = turnRollSnapshots[dice.id] ?: return
            turnRollSnapshots[dice.id] = existing.copy(color = color)
            refreshOnUi()
        }

        override fun onChargingChanged(dice: IDice, charging: Boolean) = Unit
        override fun onChargeLevel(dice: IDice, level: Int) = Unit

        override fun onDisconnected(dice: IDice) {
            syncRequiredTurnDice()
            refreshOnUi()
        }

        override fun onNewDiceDetected() {
            syncRequiredTurnDice()
            refreshOnUi()
        }

        override fun onConnectionChanged(dice: IDice, connected: Boolean) {
            syncRequiredTurnDice()
            refreshOnUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        applySystemBars()
        applySafeAreaInsets(findViewById(R.id.constraint))

        diceManager = DiceManagerFactory.getManager()
        diceManager.addListener(diceStateListener)

        playerNameView = findViewById(R.id.playerName)
        rollInfoText = findViewById(R.id.rollInfoText)
        turnDiceSet = findViewById(R.id.turnDiceSet)
        turnDiceSet.setHeaderVisible(false)
        turnDiceSet.isClickable = false

        setupPlayers()
        bindDiceSets()
        beginTurn()

        uiHandler.post(missingDiceToggleRunnable)
        uiHandler.post(requiredDiceLedRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacks(missingDiceToggleRunnable)
        uiHandler.removeCallbacks(requiredDiceLedRunnable)
        diceManager.removeListener(diceStateListener)
    }

    private fun bindDiceSets() {
        val grid = findViewById<GridLayout>(R.id.diceGrid)
        val combinationsByTitle = eYahtzeeCombination.entries
            .associateBy { getString(it.displayNameRes) }

        for (index in 0 until grid.childCount) {
            val child = grid.getChildAt(index)
            if (child is DiceSet) {
                val title = child.findViewById<TextView>(R.id.title)?.text?.toString()
                val combination = title?.let { combinationsByTitle[it] }
                if (combination != null) {
                    diceSetsByCombination[combination] = child
                    child.setOnClickListener { handleDiceSetClick(combination) }
                } else {
                    Log.w("GameActivity", "DiceSet title not mapped to combination: $title")
                }
            }
        }
        Log.d("GameActivity", "Mapped dice sets: ${diceSetsByCombination.keys}")
    }

    private fun syncRequiredTurnDice() {
        val newRequiredIds = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .sortedBy { it.id }
            .take(REQUIRED_DICE_COUNT)
            .map { it.id }

        if (requiredTurnDiceIds == newRequiredIds) return

        requiredTurnDiceIds.clear()
        requiredTurnDiceIds.addAll(newRequiredIds)
        turnRollSnapshots.keys.retainAll(newRequiredIds.toSet())
    }

    private fun beginTurn() {
        syncRequiredTurnDice()
        turnRollSnapshots.clear()
        missingDiceToggleOn = false
        refreshDiceSetsFromManager()
    }

    private fun refreshDiceSetsFromManager() {
        syncRequiredTurnDice()

        val player = currentPlayer()
        val turnReadyToScore = isTurnReadyToScore()
        val turnFaces = turnFacesForScoring()
        val turnDisplaySnapshots = buildTurnDisplaySnapshots()

        diceSetsByCombination.forEach { (combination, diceSet) ->
            val lockedSnapshot = player.lockedByCombination[combination]
            if (lockedSnapshot != null) {
                diceSet.setDiceFaces(lockedSnapshot.map { it.face }, lockFaces = true)
                lockedSnapshot.forEachIndexed { index, snapshot ->
                    diceSet.setDiceColor(index, snapshot.color)
                }
                diceSet.isClickable = false
                diceSet.alpha = 1f
            } else {
                val facesForDisplay = placeholderFaces[combination] ?: emptyList()
                diceSet.unlockFaces()
                diceSet.setDiceFaces(facesForDisplay, lockFaces = false)
                for (index in 0 until REQUIRED_DICE_COUNT) {
                    diceSet.setDiceColor(index, null)
                }
                diceSet.isClickable = turnReadyToScore
                diceSet.alpha = if (turnReadyToScore) 1f else 0.85f
            }

            val score = if (lockedSnapshot != null) {
                player.scoreByCombination[combination]
                    ?: calculateScore(combination, lockedSnapshot.map { it.face })
            } else {
                calculateScore(combination, turnFaces)
            }
            diceSet.setScore(score)
        }

        turnDiceSet.unlockFaces()
        turnDiceSet.setDiceFaces(turnDisplaySnapshots.map { it.face }, lockFaces = false)
        turnDisplaySnapshots.forEachIndexed { index, snapshot ->
            turnDiceSet.setDiceColor(index, snapshot.color)
        }

        updateRollInfoText(turnReadyToScore)
    }

    private fun buildTurnDisplaySnapshots(): List<DiceSnapshot> {
        val snapshots = mutableListOf<DiceSnapshot>()
        for (index in 0 until REQUIRED_DICE_COUNT) {
            val diceId = requiredTurnDiceIds.getOrNull(index)
            val rolled = diceId?.let { turnRollSnapshots[it] }
            if (rolled != null) {
                snapshots.add(rolled)
            } else {
                snapshots.add(
                    DiceSnapshot(
                        face = if (missingDiceToggleOn) 1 else 0,
                        color = if (missingDiceToggleOn) GoDiceSDK.DICE_YELLOW else null
                    )
                )
            }
        }
        return snapshots
    }

    private fun turnFacesForScoring(): List<Int> {
        return requiredTurnDiceIds.mapNotNull { turnRollSnapshots[it]?.face }
    }

    private fun isTurnReadyToScore(): Boolean {
        return requiredTurnDiceIds.size == REQUIRED_DICE_COUNT &&
                requiredTurnDiceIds.all { turnRollSnapshots.containsKey(it) }
    }

    private fun updateRollInfoText(turnReadyToScore: Boolean) {
        if (turnReadyToScore) {
            rollInfoText.visibility = View.GONE
            return
        }

        rollInfoText.visibility = View.VISIBLE
        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT) {
            rollInfoText.text = getString(R.string.roll_all_dice_connect_five)
            return
        }

        val rolledCount = requiredTurnDiceIds.count { turnRollSnapshots.containsKey(it) }
        rollInfoText.text = getString(
            R.string.roll_all_dice_required_info,
            rolledCount,
            REQUIRED_DICE_COUNT
        )
    }

    private fun flashRequiredDiceLeds() {
        if (!::diceManager.isInitialized) return

        syncRequiredTurnDice()
        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT || isTurnReadyToScore()) return

        val missingIds = requiredTurnDiceIds.filterNot { turnRollSnapshots.containsKey(it) }
        if (missingIds.isEmpty()) return

        val connectedById = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .associateBy { it.id }

        missingIds.forEach { diceId ->
            connectedById[diceId]?.blinkLed(
                color = REQUIRED_DICE_LED_COLOR,
                onDuration = REQUIRED_DICE_LED_ON_DURATION,
                offDuration = REQUIRED_DICE_LED_OFF_DURATION,
                blinks = 1
            )
        }
    }

    private fun calculateScore(combination: eYahtzeeCombination, faces: List<Int>): Int {
        if (faces.size < REQUIRED_DICE_COUNT) return 0
        val counts = faces.groupingBy { it }.eachCount()
        return when (combination) {
            eYahtzeeCombination.ONES -> faces.count { it == 1 } * 1
            eYahtzeeCombination.TWOS -> faces.count { it == 2 } * 2
            eYahtzeeCombination.THREES -> faces.count { it == 3 } * 3
            eYahtzeeCombination.FOURS -> faces.count { it == 4 } * 4
            eYahtzeeCombination.FIVES -> faces.count { it == 5 } * 5
            eYahtzeeCombination.SIXES -> faces.count { it == 6 } * 6
            eYahtzeeCombination.THREE_OF_A_KIND -> if (counts.any { it.value >= 3 }) faces.sum() else 0
            eYahtzeeCombination.FOUR_OF_A_KIND -> if (counts.any { it.value >= 4 }) faces.sum() else 0
            eYahtzeeCombination.FULL_HOUSE -> {
                val freq = counts.values.sorted()
                if (freq == listOf(2, 3)) 25 else 0
            }

            eYahtzeeCombination.SMALL_STRAIGHT -> if (hasStraight(faces, 4)) 30 else 0
            eYahtzeeCombination.LARGE_STRAIGHT -> if (hasStraight(faces, 5)) 40 else 0
            eYahtzeeCombination.YAHTZEE -> if (faces.distinct().size == 1) 50 else 0
            eYahtzeeCombination.CHANCE -> faces.sum()
        }
    }

    private fun hasStraight(faces: List<Int>, length: Int): Boolean =
        faces.toSet().sorted().windowed(length, 1).any { seq ->
            seq.zipWithNext().all { (a, b) -> b == a + 1 }
        }

    private fun refreshOnUi() {
        runOnUiThread { refreshDiceSetsFromManager() }
    }

    private fun handleDiceSetClick(combination: eYahtzeeCombination) {
        val player = currentPlayer()
        if (player.lockedByCombination.containsKey(combination)) return

        if (!isTurnReadyToScore()) {
            Toast.makeText(
                this,
                getString(R.string.roll_all_dice_required_to_score),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val snapshots = requiredTurnDiceIds
            .mapNotNull { turnRollSnapshots[it] }
            .take(REQUIRED_DICE_COUNT)
            .sortedBy { it.face }

        if (snapshots.size < REQUIRED_DICE_COUNT) {
            Toast.makeText(
                this,
                getString(R.string.roll_all_dice_connect_five),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        player.lockedByCombination[combination] = snapshots
        player.scoreByCombination[combination] =
            calculateScore(combination, snapshots.map { it.face })

        if (isGameComplete()) {
            showResults()
        } else {
            advanceToNextPlayer()
        }
    }

    private fun setupPlayers() {
        val names = intent.getStringArrayListExtra(EXTRA_PLAYER_NAMES)
            ?.filter { it.isNotBlank() }
            ?.take(MAX_PLAYERS)
            ?: emptyList()
        val resolvedNames = if (names.isEmpty()) listOf("Player 1") else names
        players.clear()
        players.addAll(resolvedNames.map { PlayerState(it) })
        currentPlayerIndex = 0
        updatePlayerLabel()
    }

    private fun currentPlayer(): PlayerState = players[currentPlayerIndex]

    private fun updatePlayerLabel() {
        playerNameView.text = currentPlayer().name
    }

    private fun advanceToNextPlayer() {
        if (players.size > 1) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
            updatePlayerLabel()
            Toast.makeText(
                this,
                getString(R.string.next_player_turn, currentPlayer().name),
                Toast.LENGTH_SHORT
            ).show()
        }
        beginTurn()
    }

    private fun isGameComplete(): Boolean {
        val requiredCount = eYahtzeeCombination.entries.size
        return players.isNotEmpty() &&
                players.all { it.lockedByCombination.size == requiredCount }
    }

    private fun showResults() {
        val totals = players.map { player ->
            eYahtzeeCombination.entries.sumOf { combination ->
                player.scoreByCombination[combination] ?: 0
            }
        }
        val intent = android.content.Intent(this, ResultsActivity::class.java)
            .putStringArrayListExtra(
                ResultsActivity.EXTRA_PLAYER_NAMES,
                ArrayList(players.map { it.name })
            )
            .putIntegerArrayListExtra(
                ResultsActivity.EXTRA_PLAYER_SCORES,
                ArrayList(totals)
            )
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

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

    private fun applySystemBars() {
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private data class DiceSnapshot(
        val face: Int,
        val color: Int?
    )

    private data class PlayerState(
        val name: String,
        val lockedByCombination: MutableMap<eYahtzeeCombination, List<DiceSnapshot>> =
            mutableMapOf(),
        val scoreByCombination: MutableMap<eYahtzeeCombination, Int> = mutableMapOf()
    )

    companion object {
        private const val MAX_PLAYERS = 4
        private const val REQUIRED_DICE_COUNT = 5
        private const val MISSING_DICE_TOGGLE_INTERVAL_MS = 550L
        private const val REQUIRED_DICE_LED_FLASH_INTERVAL_MS = 2200L
        private const val REQUIRED_DICE_LED_COLOR = 0xFFE300
        private const val REQUIRED_DICE_LED_ON_DURATION = 0.12f
        private const val REQUIRED_DICE_LED_OFF_DURATION = 0.12f

        const val EXTRA_PLAYER_NAMES = "com.example.godicetest.player_names"

        fun createIntent(context: android.content.Context, names: ArrayList<String>) =
            android.content.Intent(context, GameActivity::class.java)
                .putStringArrayListExtra(EXTRA_PLAYER_NAMES, names)
    }
}
