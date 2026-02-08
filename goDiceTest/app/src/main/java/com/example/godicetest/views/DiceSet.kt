package com.example.godicetest.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import com.example.godicetest.R
import com.example.godicetest.adapters.getColorHex
import com.example.godicetest.enums.eDiceNeonColor
import com.example.godicetest.enums.eDicePattern
import com.example.godicetest.extensions.setNeonColor
import com.example.godicetest.extensions.setNeonGlow
import com.example.godicetest.interfaces.IDice
import kotlin.math.roundToInt

class DiceSet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val titleView: TextView
    private val scoreView: TextView
    private val headerContainer: View
    private val diceContainer: LinearLayout
    private val diceSlots = mutableListOf<DiceSlot>()
    private val baseDiceContainerPaddingLeft: Int
    private val baseDiceContainerPaddingTop: Int
    private val baseDiceContainerPaddingRight: Int
    private val baseDiceContainerPaddingBottom: Int
    private var facesLocked = false
    private var onDiceSlotClick: ((Int) -> Unit)? = null

    private data class DiceSlot(
        val root: View,
        val grid: NeonGridLayout,
        val cells: List<View>,
        val useSmallDots: Boolean = false,
        var face: Int = 0,
        var color: Int? = null,
        var lowered: Boolean = false
    )

    companion object {
        private const val DICE_COUNT = 5
    }

    fun unlockFaces() {
        facesLocked = false
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.dice_set, this, true)
        orientation = VERTICAL
        titleView = findViewById(R.id.title)
        scoreView = findViewById(R.id.score)
        headerContainer = findViewById(R.id.headerContainer)
        diceContainer = findViewById(R.id.diceContainer)
        baseDiceContainerPaddingLeft = diceContainer.paddingLeft
        baseDiceContainerPaddingTop = diceContainer.paddingTop
        baseDiceContainerPaddingRight = diceContainer.paddingRight
        baseDiceContainerPaddingBottom = diceContainer.paddingBottom
        clipChildren = false
        clipToPadding = false
        diceContainer.clipChildren = false
        diceContainer.clipToPadding = false
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        titleView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        scoreView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        diceContainer.importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addDiceSlots(DICE_COUNT)
        enforceSquareSlots()

        context.theme.obtainStyledAttributes(attrs, R.styleable.TitleBox, 0, 0)
            .apply {
                try {
                    val text = getString(R.styleable.TitleBox_title)
                    if (text != null) titleView.text = text
                } finally {
                    recycle()
                }
            }
        updateAccessibilityLabel()
    }

    fun setTitle(text: String) {
        titleView.text = text
        updateAccessibilityLabel()
    }

    fun setHeaderVisible(visible: Boolean) {
        headerContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setOnDiceSlotClickListener(listener: ((Int) -> Unit)?) {
        onDiceSlotClick = listener
    }

    fun setDiceLowered(index: Int, lowered: Boolean, offsetDp: Int = 10) {
        if (index !in diceSlots.indices) return
        val slot = diceSlots[index]
        slot.lowered = lowered
        slot.root.translationY = if (lowered) dpToPx(offsetDp).toFloat() else 0f
        updateLoweredInset(offsetDp)
    }

    /**
     * Sets the face values for the dice in this set.
     * Expects up to five entries; missing slots are cleared.
     */
    fun setDiceFaces(faces: List<Int>, lockFaces: Boolean = false) {
        if (lockFaces) facesLocked = true
        val limitedFaces = faces.take(DICE_COUNT)
        limitedFaces.forEachIndexed { index, face ->
            diceSlots.getOrNull(index)?.let { slot ->
                slot.face = face
                updateSlot(slot)
            }
        }
        if (limitedFaces.size < diceSlots.size) {
            for (index in limitedFaces.size until diceSlots.size) {
                val slot = diceSlots[index]
                slot.face = 0
                slot.color = null
                updateSlot(slot)
            }
        }
    }

    /**
     * Locks the set with exactly five dice objects, snapshotting face and color.
     * Further automatic updates will be ignored until unlockFaces() is called.
     */
    fun setDiceResults(diceList: List<IDice>) {
        require(diceList.size == DICE_COUNT) {
            "DiceSet requires exactly $DICE_COUNT dice to set results"
        }
        facesLocked = true
        diceList.sortedBy { dice -> dice.lastRoll.value }
            .forEachIndexed { index, dice ->
                diceSlots.getOrNull(index)?.let { slot ->
                    slot.face = dice.lastRoll.value ?: 0
                    slot.color = dice.color.value
                    updateSlot(slot)
                }
            }
    }

    fun isLocked(): Boolean = facesLocked

    fun getFaces(): List<Int> = diceSlots.map { it.face }

    fun setScore(score: Int) {
        scoreView.text = score.toString()
        updateAccessibilityLabel()
    }

    /**
     * Updates the color of a specific dice slot using the GoDice color int.
     */
    fun setDiceColor(index: Int, color: Int?) {
        if (index !in diceSlots.indices) return
        val slot = diceSlots[index]
        slot.color = color
        updateSlot(slot)
    }

    private fun addDiceSlots(count: Int) {
        repeat(count) { slotIndex ->
            val diceView = LayoutInflater.from(context)
                .inflate(R.layout.dice_view_small, diceContainer, false)

            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                val margin = dpToPx(4)
                marginEnd = if (slotIndex == count - 1) 0 else margin
            }
            diceView.layoutParams = params

            val grid = diceView.findViewById<NeonGridLayout>(R.id.diceGrid)
            val cells = listOf<View>(
                diceView.findViewById(R.id.cell_00),
                diceView.findViewById(R.id.cell_01),
                diceView.findViewById(R.id.cell_02),
                diceView.findViewById(R.id.cell_10),
                diceView.findViewById(R.id.cell_11),
                diceView.findViewById(R.id.cell_12),
                diceView.findViewById(R.id.cell_20),
                diceView.findViewById(R.id.cell_21),
                diceView.findViewById(R.id.cell_22)
            )
            grid.setOnClickListener { onDiceSlotClick?.invoke(slotIndex) }
            diceView.setOnClickListener { onDiceSlotClick?.invoke(slotIndex) }
            diceContainer.addView(diceView)
            diceSlots.add(DiceSlot(diceView, grid, cells, useSmallDots = true))
        }
    }

    private fun enforceSquareSlots() {
        diceContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val availableWidth = diceContainer.width -
                            diceContainer.paddingStart - diceContainer.paddingEnd
                    if (availableWidth <= 0) return

                    val margin = dpToPx(4)
                    val targetSize = (availableWidth - margin * (DICE_COUNT - 1)) / DICE_COUNT
                    if (targetSize <= 0) return

                    diceSlots.forEachIndexed { index, slot ->
                        val baseSize = slot.grid.measuredHeight.takeIf { it > 0 }
                            ?: slot.grid.measuredWidth.takeIf { it > 0 }
                            ?: targetSize
                        val size = minOf(baseSize, targetSize)

                        (slot.grid.layoutParams as LayoutParams).apply {
                            width = size
                            height = size
                            weight = 0f
                            marginEnd = if (index == diceSlots.lastIndex) 0 else margin
                        }.also { slot.grid.layoutParams = it }
                    }
                    diceContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    private fun updateSlot(slot: DiceSlot) {
        val pattern = faceToPattern(slot.face)
        val colorHex = getColorHex(slot.color ?: eDiceNeonColor.Unknown.intColor)

        val strokeRes = if (slot.useSmallDots) R.drawable.stroke_bg_small else R.drawable.stroke_bg
        val strokeWidth = if (slot.useSmallDots) dpToPx(1) else 8
        slot.grid.setNeonGlow(colorHex, strokeWidth = strokeWidth, strokeDrawable = strokeRes)
        slot.grid.setNeonColor(colorHex)

        slot.cells.zip(pattern).forEach { (cell, isDot) ->
            val drawableRes = if (slot.useSmallDots) {
                if (isDot) R.drawable.dice_dot_on_small else R.drawable.dice_dot_off_small
            } else {
                if (isDot) R.drawable.dice_dot_on else R.drawable.dice_dot_off
            }
            cell.setBackgroundResource(drawableRes)
            if (isDot) {
                cell.setNeonColor(colorHex)
            }
        }
    }

    private fun updateAccessibilityLabel() {
        val title = titleView.text?.toString()?.trim().orEmpty()
        val score = scoreView.text?.toString()?.trim().orEmpty()
        contentDescription = when {
            title.isNotEmpty() && score.isNotEmpty() -> "$title, score $score"
            title.isNotEmpty() -> title
            score.isNotEmpty() -> "score $score"
            else -> ""
        }
    }

    private fun faceToPattern(face: Int): List<Boolean> {
        return when (face) {
            1 -> eDicePattern.Dice_1.pattern
            2 -> eDicePattern.Dice_2.pattern
            3 -> eDicePattern.Dice_3.pattern
            4 -> eDicePattern.Dice_4.pattern
            5 -> eDicePattern.Dice_5.pattern
            6 -> eDicePattern.Dice_6.pattern
            else -> eDicePattern.Dice_0.pattern
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    private fun updateLoweredInset(offsetDp: Int) {
        val extraBottom = if (diceSlots.any { it.lowered }) dpToPx(offsetDp) else 0
        diceContainer.setPadding(
            baseDiceContainerPaddingLeft,
            baseDiceContainerPaddingTop,
            baseDiceContainerPaddingRight,
            baseDiceContainerPaddingBottom + extraBottom
        )
    }
}
