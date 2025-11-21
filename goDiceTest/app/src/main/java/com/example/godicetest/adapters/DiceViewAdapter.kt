package com.example.godicetest.adapters

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.R
import com.example.godicetest.enums.eDiceNeonColor
import com.example.godicetest.enums.eDicePattern
import com.example.godicetest.extensions.setNeonColor
import com.example.godicetest.extensions.setNeonGlow
import com.example.godicetest.interfaces.IDice
import com.example.godicetest.views.NeonGridLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.sample.godicesdklib.GoDiceSDK

/**
 * Adapter for displaying a list of Dice in a RecyclerView.
 *
 * @param diceList The list of Dice to display.
 * @param onDiceClick Callback function to handle dice click events.
 * @return RecyclerView.Adapter for DiceViewHolder.
 */
class DiceViewAdapter(
    private val diceList: List<IDice>,
    private val onDiceClick: (IDice, View) -> Unit
) : RecyclerView.Adapter<DiceViewAdapter.DiceViewHolder>() {

    //region ViewHolder
    /** ViewHolder for a single Dice item.
     *
     * @param itemView The view representing a single dice item.
     */
    inner class DiceViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var diceGrid: NeonGridLayout = itemView.findViewById(R.id.diceGrid)
        var cells: List<View> = listOf(
            itemView.findViewById(R.id.cell_00),
            itemView.findViewById(R.id.cell_01),
            itemView.findViewById(R.id.cell_02),
            itemView.findViewById(R.id.cell_10),
            itemView.findViewById(R.id.cell_11),
            itemView.findViewById(R.id.cell_12),
            itemView.findViewById(R.id.cell_20),
            itemView.findViewById(R.id.cell_21),
            itemView.findViewById(R.id.cell_22)
        )
    }
    //endregion

    //region RecyclerView.Adapter methods
    /** Creates a new DiceViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new DiceViewHolder instance.
     */
    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int
    ): DiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dice_view, parent, false)
        return DiceViewHolder(view)
    }

    /** Binds data to the DiceViewHolder at the specified position.
     *
     * @param holder The DiceViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    override fun onBindViewHolder(
        holder: DiceViewHolder,
        position: Int
    ) {
        val dice = diceList[position]
        holder.itemView.setOnClickListener {
            onDiceClick(dice, holder.itemView)
        }

        holder.itemView.tag?.let { previousJob ->
            if (previousJob is Job) previousJob.cancel()
        }


        val job = CoroutineScope(Dispatchers.Main).launch {
            combine(dice.isStable, dice.color, dice.lastRoll) { isStable, color, lastRoll ->
                Triple(isStable, color, lastRoll)
            }.collect { (isStable, color, lastRoll) ->

                val colorHex = color?.let { getColorHex(it) } ?: eDiceNeonColor.Unknown.hexCode

                // Set neon glow based on dice color
                color?.let { colorInt ->
                    holder.diceGrid.setNeonGlow(colorHex)
                }

                val pattern =
                    if (isStable == true) dice.getDicePattern()
                    else eDicePattern.Dice_0.pattern

                holder.cells.zip(pattern).forEach { (cell, isDot) ->
                    val drawableRes = if (isDot) R.drawable.dice_dot_on else R.drawable.dice_dot_off
                    cell.setBackgroundResource(drawableRes)
                    if (isDot) {
                        color?.let { colorInt ->
                            cell.setNeonColor(colorHex)
                        }
                    }
                }
            }
        }

        holder.itemView.tag = job
    }

    override fun getItemCount(): Int = diceList.size
    //endregion
}

//region Color helpers
/**
 * Returns the hexadecimal color code for the given dice color.
 *
 * @param color The dice color code.
 * @return The corresponding hexadecimal color code as a string.
 */
fun getColorHex(color: Int): String {
    return when (color) {
        GoDiceSDK.DICE_BLACK -> eDiceNeonColor.Black.hexCode
        GoDiceSDK.DICE_RED -> eDiceNeonColor.Red.hexCode
        GoDiceSDK.DICE_GREEN -> eDiceNeonColor.Green.hexCode
        GoDiceSDK.DICE_BLUE -> eDiceNeonColor.Blue.hexCode
        GoDiceSDK.DICE_YELLOW -> eDiceNeonColor.Yellow.hexCode
        GoDiceSDK.DICE_ORANGE -> eDiceNeonColor.Orange.hexCode
        else -> eDiceNeonColor.Unknown.hexCode
    }
}
//endregion

// End of adapter. Luck’s a fiery mistress tonight.
