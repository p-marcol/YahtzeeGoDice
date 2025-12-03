package com.example.godicetest.activities

import android.os.Bundle
import android.util.Log
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class GameActivity : AppCompatActivity() {

    private lateinit var diceManager: IDiceManager
    private lateinit var diceViewer: RecyclerView
    private lateinit var diceViewAdapter: DiceViewAdapter
    private val diceSetsByCombination = mutableMapOf<eYahtzeeCombination, DiceSet>()
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

        diceManager = DiceManagerFactory.getManager()
        diceManager.addListener(diceStateListener)
        bindDiceSets()
        applyPlaceholderFaces()
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
        val combinationsByTitle = eYahtzeeCombination.values()
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

    private fun applyPlaceholderFaces() {
        placeholderFaces.forEach { (combination, faces) ->
            diceSetsByCombination[combination]?.setDiceFaces(faces, lockFaces = false)
        }
    }

    private fun refreshDiceSetsFromManager() {
        val liveFaces = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .mapNotNull { it.lastRoll.value }
            .take(5)

        diceSetsByCombination.forEach { (combination, diceSet) ->
            val facesForDisplay = if (diceSet.isLocked()) {
                diceSet.getFaces()
            } else {
                // Show best-scoring pattern per category until user locks a real roll
                placeholderFaces[combination] ?: emptyList()
            }
            val facesForScore = if (diceSet.isLocked()) diceSet.getFaces() else liveFaces
            if (!diceSet.isLocked()) {
                diceSet.setDiceFaces(facesForDisplay, lockFaces = false)
            }
            val score = calculateScore(combination, facesForScore)
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
        if (diceSet.isLocked()) return

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

        diceSet.setDiceResults(diceForResult)
        diceSet.setScore(calculateScore(combination, diceSet.getFaces()))
        diceSet.isClickable = false
    }
}
