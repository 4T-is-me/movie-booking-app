package com.example.movie_booking_app.view.activity.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.api.ImportOrderItemRequest
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.data.model.entity.Supplier
import com.example.movie_booking_app.databinding.ActivityManagerCreateImportOrderBinding
import com.example.movie_booking_app.view.adapter.manager.ImportOrderItemInputAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerImportOrderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class ManagerCreateImportOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerCreateImportOrderBinding
    private val viewModel: ManagerImportOrderViewModel by viewModels()

    private lateinit var itemInputAdapter: ImportOrderItemInputAdapter
    private var selectedSupplier: Supplier? = null
    private var supplierList: List<Supplier> = emptyList()
    private var inventoryList: List<Inventory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerCreateImportOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupItemsRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupItemsRecyclerView() {
        itemInputAdapter = ImportOrderItemInputAdapter(
            onTotalChanged = { total -> updateTotalAmount(total) }
        )
        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(this@ManagerCreateImportOrderActivity)
            adapter = itemInputAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupButtons() {
        binding.btnAddItem.setOnClickListener {
            if (inventoryList.isEmpty()) {
                Toast.makeText(this, "Loading products list...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddItemDialog()
        }

        binding.btnSubmit.setOnClickListener {
            submitOrder()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    // Setup supplier dropdown
                    if (state.suppliers.isNotEmpty() && supplierList.isEmpty()) {
                        supplierList = state.suppliers
                        setupSupplierDropdown(supplierList)
                    }

                    // Load inventory items when suppliers are ready
                    if (state.inventoryItems.isEmpty() && inventoryList.isEmpty()) {
                        // Trigger from dashboard cinemaId - use manager's cinemaId via API
                        // InventoryItems loaded on demand via viewModel
                    }
                    if (state.inventoryItems.isNotEmpty()) {
                        inventoryList = state.inventoryItems
                    }

                    state.successMessage?.let {
                        Toast.makeText(this@ManagerCreateImportOrderActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                        setResult(RESULT_OK)
                        finish()
                    }
                    state.error?.let {
                        Toast.makeText(this@ManagerCreateImportOrderActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun setupSupplierDropdown(suppliers: List<Supplier>) {
        val names = suppliers.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.actvSupplier.setAdapter(adapter)
        binding.actvSupplier.setOnItemClickListener { _, _, position, _ ->
            selectedSupplier = suppliers[position]
        }
    }

    private fun showAddItemDialog() {
        val items = inventoryList
        val names = items.map { "${it.itemName} (${it.itemCode})" }.toTypedArray()
        var selectedIndex = -1

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Concession Product")
            .setSingleChoiceItems(names, -1) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Select") { _, _ ->
                if (selectedIndex >= 0) {
                    val inv = items[selectedIndex]
                    // Check if already in list
                    if (itemInputAdapter.currentList.any { it.itemCode == inv.itemCode }) {
                        Toast.makeText(this, "${inv.itemName} is already in the list", Toast.LENGTH_SHORT).show()
                    } else {
                        val newItem = ImportOrderItemRequest(
                            itemCode = inv.itemCode,
                            itemName = inv.itemName,
                            quantity = 1,
                            purchasePrice = 0L
                        )
                        itemInputAdapter.addItem(newItem)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTotalAmount(total: Long) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        binding.tvTotalAmount.text = "Total Amount: ${formatter.format(total)}"
    }

    private fun submitOrder() {
        val supplier = selectedSupplier
        if (supplier == null) {
            Toast.makeText(this, "Please select a supplier", Toast.LENGTH_SHORT).show()
            return
        }
        val items = itemInputAdapter.currentList
        if (items.isEmpty()) {
            Toast.makeText(this, "Please add at least 1 product", Toast.LENGTH_SHORT).show()
            return
        }
        val invalidItem = items.firstOrNull { it.quantity <= 0 || it.purchasePrice <= 0 }
        if (invalidItem != null) {
            Toast.makeText(this, "Please enter valid quantity and price for ${invalidItem.itemName}", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = binding.etNotes.text.toString().trim().takeIf { it.isNotEmpty() }
        viewModel.createImportOrder(supplier.id, items, notes)
    }
}
