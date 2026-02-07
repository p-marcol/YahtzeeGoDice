package com.example.godicetest.activities

import android.graphics.Color
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.R
import com.example.godicetest.adapters.DiceViewAdapter
import com.example.godicetest.enums.eYahtzeeCombination
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.interfaces.IDiceStateListener
import com.example.godicetest.managers.DiceManagerFactory
import com.example.godicetest.views.DiceSet
import kotlin.math.max

class GameActivity : AppCompatActivity() {

    private lateinit var diceManager: IDiceManager
    private lateinit var diceViewer: RecyclerView
    private lateinit var diceViewAdapter: DiceViewAdapter
    private lateinit var playerNameView: TextView
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
    private val diceStateListener = object : IDiceStateListener {
        override fun onStable(dice: IDice, face: Int) = refreshOnUi()
        override fun onRolling(dice: IDice) = Unit
        override fun onColorChanged(dice: IDice, color: Int) = Unit
        override fun onChargingChanged(dice: IDice, charging: Boolean) = Unit
        override fun onChargeLevel(dice: IDice, level: Int) = Unit
        override fun onDisconnected(dice: IDice) = refreshOnUi()
        override fun onNewDiceDetected() = refreshOnUi()
        override fun onConnectionChanged(dice: IDice, connected: Boolean) = refreshOnUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        applySystemBars()
        applySafeAreaInsets(findViewById(R.id.constraint))

        diceManager = DiceManagerFactory.getManager()
        diceManager.addListener(diceStateListener)
        playerNameView = findViewById(R.id.playerName)
        setupPlayers()
        bindDiceSets()
        refreshDiceSetsFromManager()

        diceViewer = findViewById(R.id.diceViewer)
        diceViewer.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )

        diceViewAdapter = DiceViewAdapter(
            diceManager.getAllDice().filter { dice -> diceManager.isConnected(dice) },
            onDiceClick = { dice, view -> dice.blinkLed(0x00ff00) }
        )

        diceViewer.adapter = diceViewAdapter
        Log.d("GameActivity", "Dice viewer initialized with connected dice.")
        Log.d(
            "GameActivity",
            "Connected dice count: ${
                diceManager.getAllDice().filter { dice -> diceManager.isConnected(dice) }
            }"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
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
                    child.setOnClickListener { handleDiceSetClick(child, combination) }
                } else {
                    Log.w("GameActivity", "DiceSet title not mapped to combination: $title")
                }
            }
        }
        Log.d("GameActivity", "Mapped dice sets: ${diceSetsByCombination.keys}")
        refreshDiceSetsFromManager()
    }

    private fun refreshDiceSetsFromManager() {
        val player = currentPlayer()
        val liveFaces = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .mapNotNull { it.lastRoll.value }
            .take(5)

        diceSetsByCombination.forEach { (combination, diceSet) ->
            val lockedSnapshot = player.lockedByCombination[combination]
            val facesForDisplay = if (lockedSnapshot != null) {
                lockedSnapshot.map { it.face }
            } else {
                // Show best-scoring pattern per category until user locks a real roll
                placeholderFaces[combination] ?: emptyList()
            }
            if (lockedSnapshot != null) {
                diceSet.setDiceFaces(facesForDisplay, lockFaces = true)
                lockedSnapshot.forEachIndexed { index, snapshot ->
                    diceSet.setDiceColor(index, snapshot.color)
                }
                diceSet.isClickable = false
            } else {
                diceSet.unlockFaces()
                diceSet.setDiceFaces(facesForDisplay, lockFaces = false)
                for (index in 0 until 5) {
                    diceSet.setDiceColor(index, null)
                }
                diceSet.isClickable = true
            }
            val score = if (lockedSnapshot != null) {
                player.scoreByCombination[combination]
                    ?: calculateScore(combination, facesForDisplay)
            } else {
                calculateScore(combination, liveFaces)
            }
            diceSet.setScore(score)
        }
    }

    private fun calculateScore(combination: eYahtzeeCombination, faces: List<Int>): Int {
        if (faces.size < 5) return 0
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

    private fun handleDiceSetClick(diceSet: DiceSet, combination: eYahtzeeCombination) {
        val player = currentPlayer()
        if (player.lockedByCombination.containsKey(combination)) return

        val diceForResult = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .take(5)

        if (diceForResult.size < 5) {
            Toast.makeText(
                this,
                getString(R.string.not_enough_dice_message),
                Toast.LENGTH_SHORT
            ).show()
            Log.w("GameActivity", "Not enough connected dice to lock result for $combination")
            return
        }

        val snapshots = diceForResult
            .sortedBy { it.lastRoll.value ?: 0 }
            .map { dice ->
                DiceSnapshot(dice.lastRoll.value ?: 0, dice.color.value)
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
        refreshDiceSetsFromManager()
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
        const val EXTRA_PLAYER_NAMES = "com.example.godicetest.player_names"

        fun createIntent(context: android.content.Context, names: ArrayList<String>) =
            android.content.Intent(context, GameActivity::class.java)
                .putStringArrayListExtra(EXTRA_PLAYER_NAMES, names)
    }
}
