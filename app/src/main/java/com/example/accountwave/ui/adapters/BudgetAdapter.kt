package com.example.accountwave.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accountwave.R
import com.example.accountwave.model.Budget
import java.text.NumberFormat
import java.util.Locale

class BudgetAdapter : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    private var budgets = emptyList<Budget>()

    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textLimit: TextView = itemView.findViewById(R.id.textLimit)

        fun bind(budget: Budget) {
            textCategory.text = budget.category
            textLimit.text = "Limit: ${NumberFormat.getCurrencyInstance(Locale("en", "GH")).format(budget.limit)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val currentBudget = budgets[position]
        holder.bind(currentBudget)
    }

    override fun getItemCount(): Int = budgets.size

    fun updateBudgets(newBudgets: List<Budget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }
}