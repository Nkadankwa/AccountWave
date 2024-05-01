package com.example.accountwave.ui

import SharedViewModel
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class SalesFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var editTextDate: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var editTextTitle: EditText
    private lateinit var editTextDesc: EditText
    private lateinit var tabSpinner: Spinner
    private lateinit var buttonSaveExpense: Button
    private lateinit var btnCamera: Button
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (!permissionGranted) {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        editTextDate = view.findViewById(R.id.editTextDate)
        editTextAmount = view.findViewById(R.id.editTextAmount)
        categorySpinner = view.findViewById(R.id.CategorySpinner)
        editTextTitle = view.findViewById(R.id.editTextTitle)
        editTextDesc= view.findViewById(R.id.editTextDesc)
        tabSpinner = view.findViewById(R.id.TabSpinner)
        buttonSaveExpense = view.findViewById(R.id.buttonSaveExpense)
        btnCamera = view.findViewById(R.id.btnCamera)

        loadCategories()
        loadTabs()

        btnCamera.setOnClickListener {
            showScanModeSelectionDialog()
        }

        buttonSaveExpense.setOnClickListener {
            saveExpense()
        }

        requestCameraPermission()

        setFragmentResultListener("barcodeResult") { _, bundle ->
            val barcode = bundle.getString("barcode")
            barcode?.let {
                Toast.makeText(requireContext(), "Barcode Received: $it", Toast.LENGTH_SHORT).show()
                editTextTitle.setText(it)
                editTextDesc.text.clear()
                editTextAmount.setText("")
            }
        }

        setFragmentResultListener("receiptResult") { _, bundle ->
            val receiptText = bundle.getString("receipt_text")
            receiptText?.let {
                Toast.makeText(requireContext(), "Receipt Text Received", Toast.LENGTH_SHORT).show()
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                editTextTitle.setText("Receipt - $currentDate")
                editTextDesc.setText(it)
                val totalCost = extractTotalAmount(it)
                editTextAmount.setText(totalCost)
            }
        }
    }

    private fun extractTotalAmount(receiptText: String): String {
        val pattern = Pattern.compile("(?:Total|Amount Due|Total Due|Sum)\\s*[\\D]?\\s*([\\d.,]+)")
        val matcher = pattern.matcher(receiptText)
        if (matcher.find()) {
            return matcher.group(1).replace(",", "")
        }
        return ""
    }

    private fun showScanModeSelectionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Scan Mode")
            .setItems(arrayOf("Barcode", "Receipt")) { _, which ->
                val scanMode = which + 1
                val action = SalesFragmentDirections.actionTransactionToBarcodeScanner(scanMode)
                findNavController().navigate(action)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionResult.launch(Manifest.permission.CAMERA)
        } else {
            Log.d("SalesFragment", "Camera permission already granted")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun loadCategories() {
        runBlocking {
            val budgetCategories = database.budgetDao().getAllOnce().map { it.category }.toTypedArray()
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                budgetCategories
            )
            categorySpinner.adapter = categoryAdapter
        }
    }

    private fun loadTabs() {
        runBlocking {
            val tabNames = database.tabDao().getAllOnce().map { it.name }.toTypedArray()
            val tabAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                tabNames
            )
            tabSpinner.adapter = tabAdapter
        }
    }

    private fun saveExpense() {
        val dateString = editTextDate.text.toString()
        val amountString = editTextAmount.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val title = editTextTitle.text.toString()
        val tabName = tabSpinner.selectedItem.toString()

        if (dateString.isNotEmpty() && amountString.isNotEmpty() && title.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(dateString) as Date
                val amount = amountString.toDouble()

                val transaction = Transaction(
                    date = date,
                    amount = amount,
                    category = category,
                    title = title,
                    type = "1",
                    tabName = tabName
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        database.transactionDao().insert(transaction)
                        sharedViewModel.onTransactionAdded()

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("YourFragment", "Error saving data", e)
                        }
                    }
                }

                Toast.makeText(requireContext(), "Sale/Expense saved", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Invalid date or amount", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }
}