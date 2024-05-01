package com.example.accountwave.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accountwave.R
import com.example.accountwave.model.InventoryItem

class InventoryAdapter :
    ListAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemNameTextView: TextView = itemView.findViewById(R.id.itemName)
        private val itemQuantityTextView: TextView = itemView.findViewById(R.id.itemQuantity)
        private val itemCostTextView: TextView = itemView.findViewById(R.id.itemCost)

        fun bind(item: InventoryItem) {
            itemNameTextView.text = item.name
            itemQuantityTextView.text = "Qty: ${item.quantity}"
            itemCostTextView.text = "Value: ${item.quantity*item.cost}"

        }
    }
}

class InventoryDiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
    override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean {
        return oldItem == newItem
    }
}