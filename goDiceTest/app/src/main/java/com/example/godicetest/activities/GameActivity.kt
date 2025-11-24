package com.example.godicetest.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.R
import com.example.godicetest.adapters.DiceViewAdapter
import com.example.godicetest.interfaces.IDiceManager
import com.example.godicetest.managers.DiceManagerFactory

class GameActivity : AppCompatActivity() {

    private lateinit var diceManager: IDiceManager
    private lateinit var diceViewer: RecyclerView
    private lateinit var diceViewAdapter: DiceViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        diceManager = DiceManagerFactory.getManager()

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
}