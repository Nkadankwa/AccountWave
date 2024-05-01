package com.example.accountwave.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accountwave.R
import com.example.accountwave.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class SalesAdapter : ListAdapter<Transaction, SalesAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textTransactionDate)
        val amountTextView: TextView = itemView.findViewById(R.id.textTransactionAmount)
        val categoryTextView: TextView = itemView.findViewById(R.id.textTransactionCategory)
        val titleTextView: TextView = itemView.findViewById(R.id.textTransactionTitle)

        fun bind(transaction: Transaction) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateTextView.text = dateFormat.format(transaction.date)
            amountTextView.text = String.format("GH₵%.2f", transaction.amount)
            categoryTextView.text = transaction.category
            titleTextView.text = transaction.title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentTransaction = getItem(position)
        holder.bind(currentTransaction)
    }


}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}