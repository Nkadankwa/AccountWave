package com.example.accountwave.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.InventoryItem
import com.example.accountwave.model.Transaction
import com.example.accountwave.ui.adapters.InventoryAdapter
import com.example.accountwave.ui.adapters.InventoryTransactionAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryFragment : Fragment() {

    private lateinit var recyclerViewInventory: RecyclerView
    private lateinit var recyclerViewInventoryTransactions: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var database: AppDatabase
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var totalBalanceTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        totalBalanceTextView = view.findViewById(R.id.totalBalanceTextView)
        recyclerViewInventory = view.findViewById(R.id.recyclerViewInventory)
        recyclerViewInventoryTransactions = view.findViewById(R.id.recyclerViewInventoryTransactions)

        recyclerViewInventory.layoutManager = LinearLayoutManager(requireContext())
        inventoryAdapter = InventoryAdapter()
        recyclerViewInventory.adapter = inventoryAdapter

        fabAdd = view.findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }



        filterChipGroup = view.findViewById(R.id.filterChips)

        loadTabsAndCategoriesAsChips()
        loadInventoryData()

        loadInventoryTransactions()

        val transactionAdapter = InventoryTransactionAdapter(emptyList())
        recyclerViewInventoryTransactions.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewInventoryTransactions.adapter = transactionAdapter

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
                val itemToDelete = inventoryAdapter.currentList[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("OK") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                database.inventoryItemDao().delete(itemToDelete)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        inventoryAdapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewInventory)
    }

    private fun loadInventoryData() {
        lifecycleScope.launch {
            database.inventoryItemDao().getAll().collectLatest { inventoryItems ->
                inventoryAdapter.submitList(inventoryItems)
            }
            calculateInventoryBalance()
        }
    }

    private fun calculateInventoryBalance() {
        lifecycleScope.launch {
            val inventoryItemsFlow = database.inventoryItemDao().getAll()
            val inventoryTransactionsFlow = database.transactionDao().getAll()

            launch {
                inventoryItemsFlow.collectLatest { inventoryItems ->
                    inventoryTransactionsFlow.collectLatest { transactions ->
                        val totalInventoryValue = inventoryItems.sumOf { it.quantity * it.cost }

                        totalBalanceTextView.text = "GH₵%.2f".format(totalInventoryValue)
                    }
                }
            }
        }
    }
    private fun loadInventoryTransactions() {
        lifecycleScope.launch {
            database.transactionDao().getAll().collectLatest { allTransactions ->
                val inventoryTransactions = allTransactions.filter { it.type == "2" }
                (recyclerViewInventoryTransactions.adapter as? InventoryTransactionAdapter)
                    ?.updateData(inventoryTransactions)
            }
        }
    }

    private fun showAddOptionsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Add New")
            .setItems(arrayOf("Add to Inventory", "Add Inventory Transaction")) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.nav_add_inventory)
                    1 -> {
                        val bundle = Bundle()
                        bundle.putString("transaction_type", "2")
                        findNavController().navigate(R.id.nav_add_inventory_transaction, bundle)
                    }
                }
            }
            .show()
    }

    private fun loadTabsAndCategoriesAsChips() {
        lifecycleScope.launch {
            val tabs = database.tabDao().getAllOnce()
            val categories = database.inventoryItemDao().getAllOnce()
                .map { it.category }
                .distinct()

            filterChipGroup.removeAllViews()

            val chipAll = Chip(requireContext()).apply {
                id = R.id.chipAll
                text = "All"
                isCheckable = true
                isChecked = true
                setOnClickListener { loadInventory() }
            }
            filterChipGroup.addView(chipAll)

            tabs.forEach { tab ->
                val chip = Chip(requireContext()).apply {
                    text = tab.name
                    isCheckable = true
                    setOnClickListener { filterInventoryByTab(tab.id) }
                }
                filterChipGroup.addView(chip)
            }

            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category
                    isCheckable = true
                    setOnClickListener { filterInventoryByCategory(category) }
                }
                filterChipGroup.addView(chip)
            }
        }
    }


    private fun loadInventory() {
        lifecycleScope.launch {
            database.inventoryItemDao().getAll().collectLatest { inventoryItems ->
                inventoryAdapter.submitList(inventoryItems)
            }
        }
    }

    private fun filterInventoryByTab(tabId: Int) {
        lifecycleScope.launch {
            database.inventoryItemDao().getAll().collectLatest { allItems ->
                val filteredData = allItems.filter { it.tabId == tabId }
                inventoryAdapter.submitList(filteredData)
            }
        }
    }
    private fun filterInventoryByCategory(category: String) {
        lifecycleScope.launch {
            database.inventoryItemDao().getAll().collectLatest { allItems ->
                val filteredData = if (category == "All") {
                    allItems
                } else {
                    allItems.filter { it.category == category }
                }
                inventoryAdapter.submitList(filteredData)
            }
        }
    }




    override fun onResume() {
        super.onResume()
        calculateInventoryBalance()     }
}