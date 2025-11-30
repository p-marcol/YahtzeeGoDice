package com.example.godicetest.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.GridLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.R
import com.example.godicetest.adapters.DiceViewAdapter
import com.example.godicetest.enums.eYahtzeeCombination
import com.example.godicetest.interfaces.IDiceManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        diceManager = DiceManagerFactory.getManager()
        bindDiceSets()
        applyPlaceholderFaces()

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
                } else {
                    Log.w("GameActivity", "DiceSet title not mapped to combination: $title")
                }
            }
        }
        Log.d("GameActivity", "Mapped dice sets: ${diceSetsByCombination.keys}")
    }

    private fun applyPlaceholderFaces() {
        placeholderFaces.forEach { (combination, faces) ->
            diceSetsByCombination[combination]?.setDiceFaces(faces)
        }
    }
}
