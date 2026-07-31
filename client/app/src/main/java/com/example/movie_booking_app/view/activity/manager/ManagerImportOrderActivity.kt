package com.example.movie_booking_app.view.activity.manager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.databinding.ActivityManagerImportOrderBinding
import com.example.movie_booking_app.view.adapter.manager.ImportOrderAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerImportOrderViewModel
import com.example.movie_booking_app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerImportOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerImportOrderBinding
    private val viewModel: ManagerImportOrderViewModel by viewModels()
    private lateinit var adapter: ImportOrderAdapter

    private val createOrderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadOrders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerImportOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ImportOrderAdapter(
            onCancelClick = { order -> showCancelConfirmDialog(order) }
        )
        binding.rvImportOrders.apply {
            layoutManager = LinearLayoutManager(this@ManagerImportOrderActivity)
            adapter = this@ManagerImportOrderActivity.adapter
        }
    }

    private var originalList: List<ImportOrder> = emptyList()

    private fun applyFilterAndSearch() {
        val query = binding.etSearch.text.toString().trim().lowercase()
        val filtered = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { order ->
                order.importCode.lowercase().contains(query) ||
                        (order.supplierId?.name?.lowercase()?.contains(query) ?: false)
            }
        }
        adapter.submitList(filtered)
        binding.tvEmpty.isVisible = !viewModel.uiState.value.isLoading && filtered.isEmpty()
    }

    private fun updateTabStyles(selectedStatus: String) {
        val activeBg = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_tab_active_gold)
        val activeTextColor = androidx.core.content.ContextCompat.getColor(this, R.color.bg_primary)
        val inactiveTextColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)

        binding.tabPending.apply {
            background = if (selectedStatus == "PENDING") activeBg else null
            setTextColor(if (selectedStatus == "PENDING") activeTextColor else inactiveTextColor)
        }
        binding.tabApproved.apply {
            background = if (selectedStatus == "APPROVED") activeBg else null
            setTextColor(if (selectedStatus == "APPROVED") activeTextColor else inactiveTextColor)
        }
        binding.tabRejected.apply {
            background = if (selectedStatus == "REJECTED") activeBg else null
            setTextColor(if (selectedStatus == "REJECTED") activeTextColor else inactiveTextColor)
        }
    }

    private fun setupFilterChips() {
        updateTabStyles("PENDING")

        binding.tabPending.setOnClickListener {
            updateTabStyles("PENDING")
            viewModel.loadOrders("PENDING")
        }
        binding.tabApproved.setOnClickListener {
            updateTabStyles("APPROVED")
            viewModel.loadOrders("APPROVED")
        }
        binding.tabRejected.setOnClickListener {
            updateTabStyles("REJECTED")
            viewModel.loadOrders("REJECTED")
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupFab() {
        binding.fabCreate.setOnClickListener {
            val intent = Intent(this, ManagerCreateImportOrderActivity::class.java)
            createOrderLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    originalList = state.orders
                    applyFilterAndSearch()

                    state.successMessage?.let {
                        Toast.makeText(this@ManagerImportOrderActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                    state.error?.let {
                        Toast.makeText(this@ManagerImportOrderActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showCancelConfirmDialog(order: ImportOrder) {
        val dialogBinding = com.example.movie_booking_app.databinding.DialogApproveImportOrderBinding.inflate(layoutInflater)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (320 * resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        dialogBinding.tvTitle.text = getString(R.string.goods_intake_cancel_title)
        dialogBinding.tvMessage.text = getString(R.string.goods_intake_cancel_msg_template, order.importCode)
        dialogBinding.btnCancel.text = getString(R.string.goods_intake_btn_back)
        dialogBinding.btnConfirm.text = getString(R.string.goods_intake_btn_cancel_order)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            viewModel.cancelImportOrder(order.id)
        }

        dialog.show()
    }
}
