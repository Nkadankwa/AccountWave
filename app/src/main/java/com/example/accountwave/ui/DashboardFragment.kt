package com.example.accountwave.ui

import SharedViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
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
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.Budget
import com.example.accountwave.ui.adapters.SalesAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.content.pm.PackageManager
import com.example.accountwave.model.LogEntry
import kotlinx.coroutines.flow.first

class DashboardFragment : Fragment() {

    private lateinit var chartComposeView: ComposeView
    private lateinit var database: AppDatabase
    private lateinit var totalBalanceTextView: TextView
    private lateinit var recentTransactionsRecyclerView: RecyclerView
    private lateinit var transactionAdapter: SalesAdapter
    private lateinit var categorySpinner: Spinner
    private lateinit var tagButton: Button
    private lateinit var btnExport: Button


    private var budgetCategories: List<String> = emptyList()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        recentTransactionsRecyclerView = view.findViewById(R.id.recentTransactionsRecyclerView)
        totalBalanceTextView = view.findViewById(R.id.totalBalanceTextView)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        chartComposeView = view.findViewById(R.id.chart_compose_view)
        tagButton = view.findViewById(R.id.btnTag)
        btnExport = view.findViewById(R.id.btnExport)

        recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = SalesAdapter()
        recentTransactionsRecyclerView.adapter = transactionAdapter

        sharedViewModel.transactionAdded.observe(viewLifecycleOwner) { added ->
            if (added) {
                val currentCategory = categorySpinner.selectedItem?.toString()
                if (!currentCategory.isNullOrEmpty()) {
                    loadDashboardData(currentCategory)
                } else if (budgetCategories.isNotEmpty()) {
                    loadDashboardData(budgetCategories.first())
                }
                sharedViewModel.resetTransactionAdded()
            }
        }

        loadBudgetCategorySpinner()

        tagButton.setOnClickListener {
            findNavController().navigate(R.id.nav_tab)
        }

        btnExport.setOnClickListener {
            exportLogs()
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = (recentTransactionsRecyclerView.adapter as SalesAdapter).currentList[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("OK") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database.transactionDao().delete(itemToDelete)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        recentTransactionsRecyclerView.adapter?.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recentTransactionsRecyclerView)
    }

    private fun loadBudgetCategorySpinner() {
        lifecycleScope.launch {
            database.budgetDao().getAll().collectLatest { budgets ->
                budgetCategories = budgets.map { it.category }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    budgetCategories
                )
                categorySpinner.adapter = adapter

                if (budgetCategories.isNotEmpty() && view != null) {
                    loadDashboardData(budgetCategories.first())
                }
            }
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = budgetCategories[position]
                loadDashboardData(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadDashboardData(selectedCategory: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val budgets = database.budgetDao().getBudgetsByCategory(selectedCategory)
            val budgetLimit = budgets.sumOf { it.limit }

            database.transactionDao().getAll().collectLatest { transactions ->
                val personalTransactions = transactions.filter { it.type == "1" }
                val filteredTransactions = personalTransactions.filter { it.category == selectedCategory }
                val totalSpent = filteredTransactions.sumOf { it.amount }
                val balance = budgetLimit - totalSpent

                totalBalanceTextView.text = "GH₵%.2f".format(balance)
                transactionAdapter.submitList(filteredTransactions.sortedByDescending { it.date }.take(5))

                renderChartForCategory(selectedCategory, budgets, filteredTransactions)
            }
        }
    }

    private fun renderChartForCategory(
        selectedCategory: String,
        budgets: List<Budget>,
        transactions: List<com.example.accountwave.model.Transaction>
    ) {
        val personalTransactionsInCategory = transactions.filter { it.type == "1" && it.category == selectedCategory }
        val categorySpending = personalTransactionsInCategory.groupBy { it.category }
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }

        chartComposeView.setContent {
            val modifier = Modifier.fillMaxWidth()
            budgets.forEach { budget ->
                val spent = categorySpending[budget.category] ?: 0.0
                BudgetChart(
                    modifier = modifier,
                    category = budget.category,
                    limit = budget.limit,
                    spent = spent
                )
            }
        }
    }

    private fun exportLogs() {
        lifecycleScope.launch {
            val logEntries = database.logEntryDao().getAllLogs().first()

            val formattedLogs = formatLogsToCsv(logEntries)

            val file = saveLogsToFile(formattedLogs)

            if (file != null) {
                shareFile(file)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving logs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatLogsToCsv(logEntries: List<LogEntry>): String {
        val csvHeader = "Timestamp,Entity Name,Entity ID,Operation Type,Details\n"
        val csvData = StringBuilder(csvHeader)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (logEntry in logEntries) {
            csvData.append(dateFormat.format(logEntry.timestamp)).append(",")
            csvData.append(logEntry.entityName).append(",")
            csvData.append(logEntry.entityId).append(",")
            csvData.append(logEntry.operationType).append(",")
            csvData.append(logEntry.details?.replace(",", ";")).append("\n")
        }

        return csvData.toString()
    }

    private fun saveLogsToFile(formattedLogs: String): File? {
        return try {
            val fileName = "logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(requireContext().filesDir, fileName)
            FileOutputStream(file).use { it.write(formattedLogs.toByteArray()) }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "AccountWave Logs")
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            val resInfoList = requireContext().packageManager
                .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                requireContext().grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

            startActivity(Intent.createChooser(shareIntent, "Share logs via"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}