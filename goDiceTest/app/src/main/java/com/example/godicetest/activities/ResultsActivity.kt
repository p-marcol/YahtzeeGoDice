package com.example.godicetest.activities

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.godicetest.R
import kotlin.math.max

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)
        applySystemBars()
        applySafeAreaInsets(findViewById(R.id.constraint))

        val names = intent.getStringArrayListExtra(EXTRA_PLAYER_NAMES) ?: arrayListOf()
        val scores = intent.getIntegerArrayListExtra(EXTRA_PLAYER_SCORES) ?: arrayListOf()
        val maxScore = scores.maxOrNull() ?: 0
        val winnerCount = scores.count { it == maxScore }

        val container = findViewById<LinearLayout>(R.id.scoreContainer)
        val inflater = LayoutInflater.from(this)
        names.forEachIndexed { index, name ->
            val row = inflater.inflate(R.layout.item_score_row, container, false)
            val score = scores.getOrNull(index) ?: 0
            row.findViewById<TextView>(R.id.playerName).text = name
            row.findViewById<TextView>(R.id.playerScore).text = score.toString()
            val winnerTag = row.findViewById<TextView>(R.id.winnerTag)
            winnerTag.visibility = if (score == maxScore && winnerCount > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
            container.addView(row)
        }
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
        const val EXTRA_PLAYER_NAMES = "com.example.godicetest.results.player_names"
        const val EXTRA_PLAYER_SCORES = "com.example.godicetest.results.player_scores"
    }
}
