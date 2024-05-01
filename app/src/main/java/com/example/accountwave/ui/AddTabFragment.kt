package com.example.accountwave.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.accountwave.R
import com.example.accountwave.data.AppDatabase
import com.example.accountwave.model.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTabFragment : Fragment() {
    private lateinit var database: AppDatabase
    private lateinit var editTextTabName: EditText
    private lateinit var buttonSaveTab: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_add_tab, container, false)     }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        editTextTabName = view.findViewById(R.id.editTextTabName)
        buttonSaveTab = view.findViewById(R.id.buttonSaveTab)

        buttonSaveTab.setOnClickListener {
            val tabName = editTextTabName.text.toString()

            if (tabName.isNotEmpty()) {
                val tab = Tab(name = tabName)

                CoroutineScope(Dispatchers.IO).launch {
                    database.tabDao().insert(tab)
                    withContext(Dispatchers.Main) {

                        findNavController().navigate(R.id.Tab_to_dashboard)
                    }
                }
            } else {
                    Toast.makeText(requireContext(), "No tags Available. Add new Tags", Toast.LENGTH_LONG).show()
                }
            }
        }
    }