// DiceAdapter.kt
// Class for displaying a list of Dice in a RecyclerView.
// Author: Piotr Marcol
package com.example.godicetest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.godicetest.extensions.setNeonGlow
import com.example.godicetest.interfaces.IDice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * RecyclerView Adapter for displaying a list of Dice.
 *
 * @param diceList The list of Dice to display.
 * @param onConnectClick Callback for when the connect button is clicked.
 * @param onInfoClick Callback for when the item is clicked for more info.
 */
class DiceAdapter(
    private val diceList: List<IDice>,
    private val onConnectClick: (IDice) -> Unit,
    private val onInfoClick: (IDice, View) -> Unit,
    private val isDiceConnected: (IDice) -> Boolean = { dice -> dice.isConnected() }
) : RecyclerView.Adapter<DiceAdapter.DiceViewHolder>() {

    /**
     * ViewHolder class for Dice items.
     * @param itemView The view representing a single Dice item.
     */
    inner class DiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDiceName)
        val tvState: TextView = itemView.findViewById(R.id.tvDiceState)
        val tvColor: TextView = itemView.findViewById(R.id.tvDiceColor)
        val btnConnect: Button = itemView.findViewById(R.id.btnConnect)
        val root: View = itemView
    }

    // region RecyclerView.Adapter methods

    /**
     * Creates a new ViewHolder for a Dice item.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new DiceViewHolder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dice, parent, false)
        return DiceViewHolder(view)
    }

    /**
     * Returns the total number of Dice items.
     *
     * @return The size of the diceList.
     */
    override fun getItemCount(): Int = diceList.size

    /**
     * Binds data to the ViewHolder for a specific position.
     *
     * @param holder The DiceViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: DiceViewHolder, position: Int) {
        val dice = diceList[position]
        holder.tvName.text = dice.getDieName()
        holder.btnConnect.setNeonGlow("#3EFCFA")
        holder.btnConnect.setOnClickListener { onConnectClick(dice) }
        holder.root.setOnClickListener { onInfoClick(dice, holder.root) }

        //reactive state updates
        holder.itemView.tag?.let { previousJob ->
            if (previousJob is Job) previousJob.cancel()
        }


        val job = CoroutineScope(Dispatchers.Main).launch {
            combine(dice.lastRoll, dice.isStable, dice.color) { roll, stable, color ->
                Triple(roll, stable, color)
            }.collect { (roll, stable) ->
                val connected = isDiceConnected(dice)
                holder.tvState.text = when {
                    !connected -> "Not connected"
                    roll == null -> "No roll"
                    stable == false -> "Rolling..."
                    stable == true -> roll.toString()
                    else -> "Unknown"
                }

                holder.tvColor.text = dice.getColorName()
                holder.btnConnect.isEnabled = !connected
                holder.btnConnect.text = if (!connected) "Connect" else "Connected"
            }

        }

        holder.itemView.tag = job
    }

    // endregion
}

// DiceAdapter.kt complete.
// If something goes wrong, blame the messenger.
