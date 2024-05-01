package com.example.accountwave.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AddInventoryFragment : Fragment() {

    private lateinit var editTextName: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextCost: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var btnScan: Button
    private lateinit var database: AppDatabase
    private lateinit var categorySpinner: Spinner
    private lateinit var tabSpinner: Spinner
    private lateinit var btnCategory: Button


    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (!permissionGranted) {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        editTextName = view.findViewById(R.id.editTextName)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        editTextQuantity = view.findViewById(R.id.Quantity)
        editTextCost = view.findViewById(R.id.editTextCost)
        btnScan = view.findViewById(R.id.btnCamera)
        val btnSave = view.findViewById<Button>(R.id.btnSaveInventory)
        categorySpinner = view.findViewById(R.id.spinnerCategory)
        tabSpinner = view.findViewById(R.id.spinnerTab)

        loadInventoryCategories()
        loadTabs()

        btnScan.setOnClickListener {
            showScanModeSelectionDialog()
        }

        btnSave.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val quantity = editTextQuantity.text.toString().trim()
            val cost = editTextCost.text.toString().trim()
            val selectedCategory = categorySpinner.selectedItem.toString()
            val selectedTabId = (tabSpinner.selectedItem as? com.example.accountwave.model.Tab)?.id ?: 0

            if (name.isEmpty() || quantity.isEmpty() || cost.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantityInt = quantity.toIntOrNull()
            val costDouble = cost.toDoubleOrNull()

            if (quantityInt == null || costDouble == null) {
                Toast.makeText(requireContext(), "Enter valid numbers for quantity and cost", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val inventoryItem = InventoryItem(
                    name = name,
                    description = description,
                    quantity = quantityInt,
                    cost = costDouble,
                    category = selectedCategory,
                    tabId = selectedTabId
                )
                database.inventoryItemDao().insert(inventoryItem)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Inventory item saved", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        requestCameraPermission()

        setFragmentResultListener("barcodeResult") { _, bundle ->
            val barcode = bundle.getString("barcode")
            barcode?.let {
                Toast.makeText(requireContext(), "Barcode Received: $it", Toast.LENGTH_SHORT).show()
                editTextName.setText(it)
                editTextDescription.text.clear()
                editTextQuantity.setText("")
                editTextCost.setText("")
            }
        }

        setFragmentResultListener("receiptResult") { _, bundle ->
            val receiptText = bundle.getString("receipt_text")
            receiptText?.let {
                Toast.makeText(requireContext(), "Receipt Text Received", Toast.LENGTH_SHORT).show()
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                editTextName.setText("Receipt - $currentDate")
                editTextDescription.setText(it)
                editTextQuantity.setText("1")
                val totalCost = extractTotalCost(it)
                editTextCost.setText(totalCost)
            }
        }

        btnCategory = view.findViewById<Button>(R.id.btnCategory)
        btnCategory.setOnClickListener {
            showAddCategoryDialog()
        }




    }

    private fun showScanModeSelectionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Scan Mode")
            .setItems(arrayOf("Barcode", "Receipt")) { _, which ->
                val scanMode = which + 1
                val action = AddInventoryFragmentDirections.actionAddInventoryToBarcode(scanMode)
                findNavController().navigate(action)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun extractTotalCost(receiptText: String): String {
        val pattern = Pattern.compile("Total\\s*([\\d.,]+)")
        val matcher = pattern.matcher(receiptText)
        if (matcher.find()) {
            return matcher.group(1).replace(",", "")
        }
        return ""
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionResult.launch(Manifest.permission.CAMERA)
        } else {
            Log.d("AddInventory", "Camera permission already granted")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun loadInventoryCategories() {
        runBlocking {
            val categories = database.inventoryItemDao().getAllOnce().map { it.category }.distinct().toMutableList()

            if (categories.isEmpty()) {
                categories.add("General")
            }

            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
            categorySpinner.adapter = categoryAdapter

            if (categories.contains("General")) {
                val generalPosition = categories.indexOf("General")
                categorySpinner.setSelection(generalPosition)
            }
        }
    }


    private fun loadTabs() {
        runBlocking {
            val tabs = database.tabDao().getAllOnce()
            val tabAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                tabs
            )
            tabSpinner.adapter = tabAdapter
        }
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Category")

        val input = EditText(requireContext())
        input.hint = "Enter category name"
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val categoryName = input.text.toString().trim()
            if (categoryName.isNotEmpty()) {
                addCategoryToSpinner(categoryName)
            } else {
                Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
    private fun addCategoryToSpinner(newCategory: String) {
        val adapter = categorySpinner.adapter as ArrayAdapter<String>
        adapter.add(newCategory)
        adapter.notifyDataSetChanged()
        categorySpinner.setSelection(adapter.getPosition(newCategory))
    }





}