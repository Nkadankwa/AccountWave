package com.example.accountwave.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accountwave.R
import com.example.accountwave.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class InventoryTransactionAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<InventoryTransactionAdapter.TransactionViewHolder>() {

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.transactionTitle)
        private val dateTextView: TextView = view.findViewById(R.id.transactionDate)
        private val amountTextView: TextView = view.findViewById(R.id.transactionAmount)

        fun bind(transaction: Transaction) {
            titleTextView.text = transaction.title
            amountTextView.text = "GH₵%.2f".format(transaction.amount)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(transaction.date)
        }
    }
}
