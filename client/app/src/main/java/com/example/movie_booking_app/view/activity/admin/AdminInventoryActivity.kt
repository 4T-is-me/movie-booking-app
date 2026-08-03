package com.example.movie_booking_app.view.activity.admin

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.databinding.ActivityAdminInventoryBinding
import com.example.movie_booking_app.view.adapter.admin.InventoryAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminInventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminInventoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminInventoryBinding
    private val viewModel: AdminInventoryViewModel by viewModels()
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        inventoryAdapter = InventoryAdapter { item ->
            showRefillDialog(item)
        }

        binding.rvInventory.apply {
            layoutManager = LinearLayoutManager(this@AdminInventoryActivity)
            adapter = inventoryAdapter
        }

        binding.actvCinema.setOnItemClickListener { _, _, position, _ ->
            val cinema = viewModel.uiState.value.cinemas.getOrNull(position)
            cinema?.let { viewModel.selectCinema(it) }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    // Setup Dropdown
                    if (state.cinemas.isNotEmpty() && binding.actvCinema.adapter == null) {
                        val names = state.cinemas.map { it.cinemaName ?: "Unknown" }
                        val adapter = ArrayAdapter(this@AdminInventoryActivity, R.layout.item_spinner_dropdown, names)
                        binding.actvCinema.setAdapter(adapter)
                        
                        // Default select first
                        if (names.isNotEmpty()) {
                            binding.actvCinema.setText(names[0], false)
                            viewModel.selectCinema(state.cinemas[0])
                        }
                    }

                    // Setup List
                    inventoryAdapter.submitList(state.inventoryItems)

                    // Error & Success Toast
                    state.error?.let {
                        Toast.makeText(this@AdminInventoryActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }

                    state.refillSuccess?.let {
                        Toast.makeText(this@AdminInventoryActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showRefillDialog(item: Inventory) {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val etQuantity = EditText(context).apply {
            hint = "Enter quantity to add"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val etReason = EditText(context).apply {
            hint = "Reason (optional)"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        layout.addView(etQuantity)
        layout.addView(etReason)

        AlertDialog.Builder(context)
            .setTitle("Refill ${item.itemName}")
            .setMessage("Current stock: ${item.quantity}")
            .setView(layout)
            .setPositiveButton("Confirm") { _, _ ->
                hideKeyboard(etQuantity)
                hideKeyboard(etReason)
                
                val qty = etQuantity.text.toString().toIntOrNull() ?: 0
                if (qty > 0) {
                    viewModel.refill(item.itemCode, qty, etReason.text.toString())
                } else {
                    Toast.makeText(context, getString(R.string.auto_str_please_enter_a_valid_quantity), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                hideKeyboard(etQuantity)
                hideKeyboard(etReason)
            }
            .show()
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
