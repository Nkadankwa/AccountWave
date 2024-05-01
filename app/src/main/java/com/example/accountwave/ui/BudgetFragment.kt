package com.example.accountwave.ui

import SharedViewModel
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.Budget
import com.example.accountwave.model.InventoryItem
import com.example.accountwave.ui.adapters.BudgetAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.widget.Toast
import android.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var editTextCategory: EditText
    private lateinit var editTextLimit: EditText
    private lateinit var buttonSaveBudget: Button
    private lateinit var buttonExpense: Button
    private lateinit var recyclerViewBudgets: RecyclerView
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var chartComposeView: ComposeView
    private lateinit var spinner: Spinner
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var selectedCategory: String? = null
    private var allBudgets: List<Budget> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        editTextCategory = view.findViewById(R.id.editTextCategory)
        editTextLimit = view.findViewById(R.id.editTextLimit)
        buttonSaveBudget = view.findViewById(R.id.buttonSaveBudget)
        buttonExpense = view.findViewById(R.id.btnExpense)
        recyclerViewBudgets = view.findViewById(R.id.recyclerViewBudgets)
        chartComposeView = view.findViewById(R.id.chart_compose_view)
        spinner = view.findViewById(R.id.spinnerCategory)

        recyclerViewBudgets.layoutManager = LinearLayoutManager(context)
        budgetAdapter = BudgetAdapter()
        recyclerViewBudgets.adapter = budgetAdapter

        buttonExpense.setOnClickListener {
            findNavController().navigate(R.id.nav_add_transaction)
        }
        buttonSaveBudget.setOnClickListener {
            saveBudget()
        }

        sharedViewModel.transactionAdded.observe(viewLifecycleOwner) { added ->
            if (added) {
                observeTransactionsAndRenderChart()
                sharedViewModel.resetTransactionAdded()
            }
        }
        setupSpinner()
    }

    private fun setupSpinner() {
        lifecycleScope.launch {
            database.budgetDao().getAll().collectLatest { budgets ->
                allBudgets = budgets
                budgetAdapter.updateBudgets(budgets)

                val categories = budgets.map { it.category }.distinct()
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    categories
                )
                spinner.adapter = spinnerAdapter

                if (categories.isNotEmpty()) {
                    selectedCategory = categories[0]
                    observeTransactionsAndRenderChart()
                }

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedCategory = categories[position]
                        observeTransactionsAndRenderChart()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                val itemTouchHelperCallback = object : SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val position = viewHolder.adapterPosition
                        val itemToDelete = budgets[position]

                        AlertDialog.Builder(context)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete this item?")
                            .setPositiveButton("OK") { _, _ ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.budgetDao().delete(itemToDelete)
                                        val updatedBudgets = database.budgetDao().getAllOnce()
                                        budgetAdapter.updateBudgets(updatedBudgets)
                                    }

                                    Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                                budgetAdapter.notifyItemChanged(position)
                            }
                            .show()
                    }
                }

                val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
                itemTouchHelper.attachToRecyclerView(recyclerViewBudgets)
            }
        }
    }

    private fun observeTransactionsAndRenderChart() {
        val category = selectedCategory ?: return
        val budgetsForCategory = allBudgets.filter { it.category == category }

        lifecycleScope.launch {
            database.transactionDao().getAll().collectLatest { transactions ->
                val personalTransactions = transactions.filter { it.type == "1" }
                val categorySpending = personalTransactions.filter { it.category == category }
                    .sumOf { it.amount }

                chartComposeView.setContent {
                    val modifier = Modifier.fillMaxWidth()
                    budgetsForCategory.forEach { budget ->
                        BudgetChart(
                            modifier = modifier,
                            category = budget.category,
                            limit = budget.limit,
                            spent = categorySpending
                        )
                    }
                }
            }
        }
    }

    private fun saveBudget() {
        val category = editTextCategory.text.toString()
        val limitStr = editTextLimit.text.toString()

        if (category.isBlank() || limitStr.isBlank()) {
            showSnackbar("Please fill in all fields")
            return
        }

        val limit = limitStr.toDoubleOrNull()
        if (limit == null) {
            showSnackbar("Please enter a valid number for limit")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val budget = Budget(category = category, limit = limit)
            database.budgetDao().insert(budget)
            withContext(Dispatchers.Main) {
                clearInputs()
                showSnackbar("Budget saved successfully")
            }}
    }

    private fun clearInputs() {
        editTextCategory.text.clear()
        editTextLimit.text.clear()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}