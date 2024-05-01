package com.example.accountwave.ui

import SharedViewModel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.InventoryItem
import com.example.accountwave.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Date

class AddInventoryTransactionFragment : Fragment() {

    private lateinit var inventoryItemSpinner: Spinner
    private lateinit var editTextQuantityChange: EditText
    private lateinit var database: AppDatabase
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var selectedInventoryItem: InventoryItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_inventory_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        inventoryItemSpinner = view.findViewById(R.id.spinnerInventoryItem)
        editTextQuantityChange = view.findViewById(R.id.editTextQuantityChange)
        val btnSaveTransaction = view.findViewById<Button>(R.id.btnSaveInventoryTransaction)

        loadInventoryItems()

        inventoryItemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedInventoryItem = parent?.getItemAtPosition(position) as? InventoryItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedInventoryItem = null
            }
        }

        btnSaveTransaction.setOnClickListener {
            saveInventoryTransaction()
        }
    }

    private fun loadInventoryItems() {
        runBlocking {
            val inventoryItems = database.inventoryItemDao().getAllOnce()
            val adapter = object : ArrayAdapter<InventoryItem>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                inventoryItems
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? android.widget.TextView)?.text = inventoryItems[position].name
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    (view as? android.widget.TextView)?.text = inventoryItems[position].name
                    return view
                }
            }

            inventoryItemSpinner.adapter = adapter

            if (inventoryItems.size == 1) {
                inventoryItemSpinner.setSelection(0)
                selectedInventoryItem = inventoryItems[0]
            }
        }
    }


    private fun saveInventoryTransaction() {
        val quantityChangeText = editTextQuantityChange.text.toString().trim()

        if (selectedInventoryItem == null) {
            Toast.makeText(requireContext(), "Please select an inventory item", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityChangeText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter the quantity change", Toast.LENGTH_SHORT).show()
            return
        }

        val quantityChange = quantityChangeText.toIntOrNull()
        if (quantityChange == null) {
            Toast.makeText(requireContext(), "Please enter a valid number for quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val currentInventoryItem = selectedInventoryItem!!
        val newQuantity = currentInventoryItem.quantity - quantityChange

        if (newQuantity < 0) {
            Toast.makeText(requireContext(), "Inventory quantity cannot be negative", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            date = Date(),
            amount = currentInventoryItem.cost * quantityChange,
            title = "${currentInventoryItem.name}",
            category = currentInventoryItem.category,
            type = "2",
            tabName = ""
        )

        val updatedInventoryItem = currentInventoryItem.copy(quantity = newQuantity)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.transactionDao().insert(transaction)
                database.inventoryItemDao().update(updatedInventoryItem)
                sharedViewModel.onTransactionAdded()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Inventory updated and transaction recorded", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("YourFragment", "Error saving data", e)
                }
            }
        }
    }
}