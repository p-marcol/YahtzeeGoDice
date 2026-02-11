package com.example.godicetest.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
    private lateinit var rerollsLeftText: TextView
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
    private val heldDiceIds = mutableSetOf<Int>()
    private var rerollsUsed = 0
    private var rerollInProgress = false
    private var currentRerollTargetIds: Set<Int> = emptySet()
    private val currentRerollRolledIds = mutableSetOf<Int>()
    private var rerollStartBlockedUntilMs = 0L
    private var pendingRollAnnouncement: String? = null

    private var activeDisplayDiceIds: List<Int?> = List(REQUIRED_DICE_COUNT) { null }

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
            handleStableEvent(dice, face)
            refreshOnUi()
        }

        override fun onRolling(dice: IDice) = Unit

        override fun onColorChanged(dice: IDice, color: Int) {
            if (dice.id in heldDiceIds) return
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
        rerollsLeftText = findViewById(R.id.rerollsLeftText)
        rollInfoText = findViewById(R.id.rollInfoText)
        turnDiceSet = findViewById(R.id.turnDiceSet)

        turnDiceSet.setHeaderVisible(false)
        turnDiceSet.setDiceSlotsAccessibilityEnabled(true)
        turnDiceSet.setOnDiceSlotClickListener { index -> onTurnDiceSlotClicked(index) }

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
        turnOffAllDiceLeds()
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
    }

    private fun beginTurn() {
        syncRequiredTurnDice()
        turnRollSnapshots.clear()
        heldDiceIds.clear()
        rerollsUsed = 0
        rerollInProgress = false
        currentRerollTargetIds = emptySet()
        currentRerollRolledIds.clear()
        rerollStartBlockedUntilMs = 0L
        activeDisplayDiceIds = List(REQUIRED_DICE_COUNT) { null }
        missingDiceToggleOn = false
        turnOffAllDiceLeds()
        refreshDiceSetsFromManager()
    }

    private fun syncRequiredTurnDice() {
        val connectedIds = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .sortedBy { it.id }
            .take(REQUIRED_DICE_COUNT)
            .map { it.id }

        if (connectedIds == requiredTurnDiceIds) return

        requiredTurnDiceIds.clear()
        requiredTurnDiceIds.addAll(connectedIds)
        val requiredSet = requiredTurnDiceIds.toSet()

        turnRollSnapshots.keys.retainAll(requiredSet)
        heldDiceIds.retainAll(requiredSet)
        currentRerollRolledIds.retainAll(requiredSet)
        currentRerollTargetIds =
            currentRerollTargetIds.filter { it in requiredSet && it !in heldDiceIds }.toSet()

        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT ||
            (rerollInProgress && currentRerollTargetIds.isEmpty())
        ) {
            rerollInProgress = false
            currentRerollTargetIds = emptySet()
            currentRerollRolledIds.clear()
            rerollStartBlockedUntilMs = 0L
        }
    }

    private fun handleStableEvent(dice: IDice, face: Int) {
        if (dice.id !in requiredTurnDiceIds) return
        if (dice.id in heldDiceIds) return

        val snapshot = DiceSnapshot(dice.id, face, dice.color.value)

        if (!hasInitialRollCompleted()) {
            if (!turnRollSnapshots.containsKey(dice.id)) {
                turnRollSnapshots[dice.id] = snapshot
                if (hasInitialRollCompleted()) {
                    rerollStartBlockedUntilMs = SystemClock.elapsedRealtime() + REROLL_START_DELAY_MS
                    queueRollAnnouncementIfReady()
                }
            }
            return
        }

        if (rerollsUsed >= MAX_REROLLS) return

        if (!rerollInProgress) {
            if (SystemClock.elapsedRealtime() < rerollStartBlockedUntilMs) return
            currentRerollTargetIds = requiredTurnDiceIds
                .filterNot { it in heldDiceIds }
                .toSet()
            if (currentRerollTargetIds.isEmpty()) return
            currentRerollRolledIds.clear()
            rerollInProgress = true
        }

        if (dice.id !in currentRerollTargetIds) return
        if (dice.id in currentRerollRolledIds) return

        turnRollSnapshots[dice.id] = snapshot
        currentRerollRolledIds.add(dice.id)

        if (currentRerollRolledIds.containsAll(currentRerollTargetIds)) {
            completeRerollCycle()
        }
    }

    private fun completeRerollCycle() {
        if (!rerollInProgress) return

        rerollInProgress = false
        currentRerollTargetIds = emptySet()
        currentRerollRolledIds.clear()
        rerollsUsed = (rerollsUsed + 1).coerceAtMost(MAX_REROLLS)
        heldDiceIds.clear()
        rerollStartBlockedUntilMs = SystemClock.elapsedRealtime() + REROLL_START_DELAY_MS

        // Requirement: when reroll completes, LEDs must be turned off for all dice.
        turnOffAllDiceLeds()
        queueRollAnnouncementIfReady()
    }

    private fun onTurnDiceSlotClicked(index: Int) {
        val diceId = activeDisplayDiceIds.getOrNull(index) ?: return
        toggleHeldDice(diceId)
    }

    private fun toggleHeldDice(diceId: Int) {
        if (diceId !in requiredTurnDiceIds) return

        if (!hasInitialRollCompleted()) {
            Toast.makeText(
                this,
                getString(R.string.roll_all_dice_required_to_score),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (rerollInProgress) {
            Toast.makeText(
                this,
                getString(R.string.reroll_wait_for_completion),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (rerollsUsed >= MAX_REROLLS) {
            Toast.makeText(
                this,
                getString(R.string.rerolls_exhausted_info),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (diceId in heldDiceIds) {
            heldDiceIds.remove(diceId)
            setDiceLed(diceId, false)
        } else {
            if (turnRollSnapshots[diceId] == null) return
            heldDiceIds.add(diceId)
            // Requirement: selected/kept dice LED should be on.
            setDiceLed(diceId, true)
        }

        refreshOnUi()
    }

    private fun setDiceLed(diceId: Int, on: Boolean) {
        diceManager.getAllDice()
            .firstOrNull { it.id == diceId && diceManager.isConnected(it) }
            ?.setLed(on)
    }

    private fun turnOffAllDiceLeds() {
        val connectedById = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .associateBy { it.id }
        requiredTurnDiceIds.forEach { id ->
            connectedById[id]?.setLed(false)
        }
    }

    private fun hasInitialRollCompleted(): Boolean {
        return requiredTurnDiceIds.size == REQUIRED_DICE_COUNT &&
                requiredTurnDiceIds.all { turnRollSnapshots.containsKey(it) }
    }

    private fun canScoreNow(): Boolean {
        return requiredTurnDiceIds.size == REQUIRED_DICE_COUNT &&
                hasInitialRollCompleted() &&
                !rerollInProgress
    }

    private fun requiredDiceIdsToRollNow(): Set<Int> {
        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT) return emptySet()
        return when {
            !hasInitialRollCompleted() -> requiredTurnDiceIds
                .filterNot { turnRollSnapshots.containsKey(it) }
                .toSet()

            rerollInProgress -> currentRerollTargetIds
                .filterNot { it in currentRerollRolledIds }
                .toSet()

            else -> emptySet()
        }
    }

    private fun shouldShowMissingForActiveDie(diceId: Int): Boolean {
        return when {
            !hasInitialRollCompleted() -> !turnRollSnapshots.containsKey(diceId)
            rerollInProgress -> diceId in currentRerollTargetIds &&
                    diceId !in currentRerollRolledIds

            else -> false
        }
    }

    private fun refreshDiceSetsFromManager() {
        syncRequiredTurnDice()

        val player = currentPlayer()
        val scoreFaces = facesForScoring()
        val canScore = canScoreNow()

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
                diceSet.isClickable = canScore
                diceSet.alpha = if (canScore) 1f else 0.85f
            }

            val score = if (lockedSnapshot != null) {
                player.scoreByCombination[combination]
                    ?: calculateScore(combination, lockedSnapshot.map { it.face })
            } else {
                calculateScore(combination, scoreFaces)
            }
            diceSet.setScore(score)
        }

        renderBottomDiceSets()
        updateRollInfoText()
        updateRerollsInfo()
    }

    private fun renderBottomDiceSets() {
        activeDisplayDiceIds = (requiredTurnDiceIds.map { it as Int? } +
                List((REQUIRED_DICE_COUNT - requiredTurnDiceIds.size).coerceAtLeast(0)) { null })
            .take(REQUIRED_DICE_COUNT)

        val displaySnapshots = activeDisplayDiceIds.map { id ->
            val snapshot = id?.let { turnRollSnapshots[it] }
            when {
                id == null -> DiceSnapshot(-1, 0, null)
                shouldShowMissingForActiveDie(id) -> DiceSnapshot(
                    id,
                    if (missingDiceToggleOn) 1 else 0,
                    if (missingDiceToggleOn) GoDiceSDK.DICE_YELLOW else null
                )

                snapshot != null -> snapshot
                else -> DiceSnapshot(id, 0, null)
            }
        }

        turnDiceSet.unlockFaces()
        turnDiceSet.setDiceFaces(displaySnapshots.map { it.face }, lockFaces = false)
        displaySnapshots.forEachIndexed { index, snapshot ->
            turnDiceSet.setDiceColor(index, snapshot.color)
            val id = activeDisplayDiceIds[index]
            turnDiceSet.setDiceLowered(index, id != null && id in heldDiceIds)
        }
        updateTurnDiceAccessibility(displaySnapshots)
    }

    private fun updateTurnDiceAccessibility(displaySnapshots: List<DiceSnapshot>) {
        val slotDescriptions = mutableListOf<String>()
        displaySnapshots.forEachIndexed { index, snapshot ->
            val diceId = activeDisplayDiceIds.getOrNull(index)
            val isBlocked = diceId != null && diceId in heldDiceIds
            val stateDescription = when {
                diceId == null -> getString(R.string.dice_slot_state_empty)
                shouldShowMissingForActiveDie(diceId) -> getString(R.string.dice_slot_state_not_rolled)
                snapshot.face in 1..6 -> getString(R.string.dice_slot_state_value, snapshot.face)
                else -> getString(R.string.dice_slot_state_not_rolled)
            }
            val blockedDescription = getString(
                if (isBlocked) R.string.dice_slot_blocked else R.string.dice_slot_not_blocked
            )
            val slotDescription = getString(
                R.string.dice_slot_accessibility_format,
                index + 1,
                stateDescription,
                blockedDescription
            )
            turnDiceSet.setDiceSlotAccessibilityDescription(index, slotDescription)
            slotDescriptions.add(slotDescription)
        }

        turnDiceSet.setAccessibilityLabelOverride(
            getString(
                R.string.turn_dice_accessibility_summary,
                slotDescriptions.joinToString(" ")
            )
        )
    }

    private fun updateRollInfoText() {
        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT) {
            rollInfoText.visibility = View.VISIBLE
            rollInfoText.text = getString(R.string.roll_all_dice_connect_five)
            return
        }

        if (!hasInitialRollCompleted()) {
            val rolledCount = requiredTurnDiceIds.count { turnRollSnapshots.containsKey(it) }
            rollInfoText.visibility = View.VISIBLE
            rollInfoText.text = getString(
                R.string.roll_all_dice_required_info,
                rolledCount,
                REQUIRED_DICE_COUNT
            )
            return
        }

        if (rerollInProgress) {
            rollInfoText.visibility = View.VISIBLE
            rollInfoText.text = getString(
                R.string.reroll_in_progress_info,
                currentRerollRolledIds.size,
                currentRerollTargetIds.size
            )
            return
        }

        rollInfoText.visibility = View.VISIBLE
        rollInfoText.text = if (rerollsUsed >= MAX_REROLLS) {
            getString(R.string.rerolls_exhausted_info)
        } else {
            getString(R.string.reroll_hint_info)
        }
    }

    private fun updateRerollsInfo() {
        val rerollsLeft = (MAX_REROLLS - rerollsUsed).coerceAtLeast(0)
        rerollsLeftText.text = getString(R.string.rerolls_left_format, rerollsLeft)
    }

    private fun flashRequiredDiceLeds() {
        if (!::diceManager.isInitialized) return

        syncRequiredTurnDice()
        val requiredToRoll = requiredDiceIdsToRollNow()
        if (requiredToRoll.isEmpty()) return

        val connectedById = diceManager.getAllDice()
            .filter { diceManager.isConnected(it) }
            .associateBy { it.id }

        requiredToRoll.forEach { id ->
            connectedById[id]?.blinkLed(
                color = REQUIRED_DICE_LED_COLOR,
                onDuration = REQUIRED_DICE_LED_ON_DURATION,
                offDuration = REQUIRED_DICE_LED_OFF_DURATION,
                blinks = 1
            )
        }
    }

    private fun facesForScoring(): List<Int> {
        if (!canScoreNow()) return emptyList()
        return requiredTurnDiceIds.mapNotNull { id -> turnRollSnapshots[id]?.face }
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
        runOnUiThread {
            refreshDiceSetsFromManager()
            pendingRollAnnouncement?.let { announcement ->
                turnDiceSet.announceForAccessibility(announcement)
                pendingRollAnnouncement = null
            }
        }
    }

    private fun queueRollAnnouncementIfReady() {
        if (requiredTurnDiceIds.size < REQUIRED_DICE_COUNT) return
        val rolledFaces = requiredTurnDiceIds.mapNotNull { id -> turnRollSnapshots[id]?.face }
        if (rolledFaces.size < REQUIRED_DICE_COUNT) return

        pendingRollAnnouncement = getString(
            R.string.roll_result_accessibility_announcement,
            rolledFaces.joinToString(", ")
        )
    }

    private fun handleDiceSetClick(combination: eYahtzeeCombination) {
        val player = currentPlayer()
        if (player.lockedByCombination.containsKey(combination)) return

        if (!canScoreNow()) {
            val messageRes = when {
                requiredTurnDiceIds.size < REQUIRED_DICE_COUNT -> R.string.roll_all_dice_connect_five
                rerollInProgress -> R.string.reroll_wait_for_completion
                else -> R.string.roll_all_dice_required_to_score
            }
            Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
            return
        }

        val snapshots = requiredTurnDiceIds
            .mapNotNull { turnRollSnapshots[it] }
            .take(REQUIRED_DICE_COUNT)
            .sortedBy { it.face }

        if (snapshots.size < REQUIRED_DICE_COUNT) {
            Toast.makeText(
                this,
                getString(R.string.roll_all_dice_required_to_score),
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
        val currentPlayerName = currentPlayer().name
        playerNameView.text = currentPlayerName
        playerNameView.contentDescription = getString(
            R.string.player_name_accessibility_format,
            currentPlayerName
        )
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
        val diceId: Int,
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
        private const val MAX_REROLLS = 2
        private const val MISSING_DICE_TOGGLE_INTERVAL_MS = 550L
        private const val REQUIRED_DICE_LED_FLASH_INTERVAL_MS = 2200L
        private const val REQUIRED_DICE_LED_COLOR = 0xFFE300
        private const val REQUIRED_DICE_LED_ON_DURATION = 0.12f
        private const val REQUIRED_DICE_LED_OFF_DURATION = 0.12f
        private const val REROLL_START_DELAY_MS = 1500L

        const val EXTRA_PLAYER_NAMES = "com.example.godicetest.player_names"

        fun createIntent(context: android.content.Context, names: ArrayList<String>) =
            android.content.Intent(context, GameActivity::class.java)
                .putStringArrayListExtra(EXTRA_PLAYER_NAMES, names)
    }
}
