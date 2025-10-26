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
import com.example.godicetest.models.Dice

/**
 * RecyclerView Adapter for displaying a list of Dice.
 * @param diceList The list of Dice to display.
 * @param onConnectClick Callback for when the connect button is clicked.
 * @param onInfoClick Callback for when the item is clicked for more info.
 */
class DiceAdapter(
    private val diceList: List<Dice>,
    private val onConnectClick: (Dice) -> Unit,
    private val onInfoClick: (Dice, View) -> Unit
) : RecyclerView.Adapter<DiceAdapter.DiceViewHolder>() {

    /**
     * ViewHolder class for Dice items.
     * @param itemView The view representing a single Dice item.
     */
    inner class DiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDiceName)
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
        holder.btnConnect.setOnClickListener { onConnectClick(dice) }
        holder.root.setOnClickListener { onInfoClick(dice, holder.root) }
    }

    // endregion
}

// DiceAdapter.kt complete.
// If something goes wrong, blame the messenger.
