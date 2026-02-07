package com.example.godicetest.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.godicetest.R
import com.example.godicetest.extensions.setNeonColor
import com.example.godicetest.extensions.setNeonGlow
import kotlin.math.max

class PlayerSetupActivity : AppCompatActivity() {

    private lateinit var playerCountLabel: TextView
    private lateinit var addPlayerButton: Button
    private lateinit var removePlayerButton: Button
    private lateinit var startGameButton: Button
    private lateinit var playerInputs: List<EditText>

    private var playerCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_setup)
        applySystemBars()
        applySafeAreaInsets(findViewById(R.id.constraint))

        playerCountLabel = findViewById(R.id.playerCountLabel)
        addPlayerButton = findViewById(R.id.addPlayerButton)
        removePlayerButton = findViewById(R.id.removePlayerButton)
        startGameButton = findViewById(R.id.startGameButton)
        playerInputs = listOf(
            findViewById(R.id.playerName1),
            findViewById(R.id.playerName2),
            findViewById(R.id.playerName3),
            findViewById(R.id.playerName4)
        )

        addPlayerButton.setNeonColor("#00FFFF")
        removePlayerButton.setNeonColor("#FF00FF")
        startGameButton.setNeonGlow("#FFFF00")

        updatePlayerInputs()

        addPlayerButton.setOnClickListener {
            if (playerCount < MAX_PLAYERS) {
                playerCount++
                updatePlayerInputs()
            }
        }

        removePlayerButton.setOnClickListener {
            if (playerCount > MIN_PLAYERS) {
                playerCount--
                updatePlayerInputs()
            }
        }

        startGameButton.setOnClickListener {
            val names = collectPlayerNames() ?: return@setOnClickListener
            val intent = GameActivity.createIntent(this, ArrayList(names))
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun updatePlayerInputs() {
        playerCountLabel.text = getString(R.string.player_count_format, playerCount)
        addPlayerButton.isEnabled = playerCount < MAX_PLAYERS
        removePlayerButton.isEnabled = playerCount > MIN_PLAYERS

        playerInputs.forEachIndexed { index, editText ->
            val visible = index < playerCount
            editText.visibility = if (visible) View.VISIBLE else View.GONE
            if (!visible) {
                editText.text?.clear()
                editText.error = null
            }
        }
    }

    private fun collectPlayerNames(): List<String>? {
        val names = mutableListOf<String>()
        for (index in 0 until playerCount) {
            val input = playerInputs[index]
            val name = input.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                input.error = getString(R.string.player_name_required)
                input.requestFocus()
                return null
            }
            names.add(name)
        }
        if (names.isEmpty()) {
            Toast.makeText(this, getString(R.string.player_name_required), Toast.LENGTH_SHORT).show()
            return null
        }
        return names
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

    companion object {
        private const val MAX_PLAYERS = 4
        private const val MIN_PLAYERS = 1
    }
}
