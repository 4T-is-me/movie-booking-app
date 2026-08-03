package com.example.movie_booking_app.view.fragment.admin

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.databinding.ActivityAdminImportOrderBinding
import com.example.movie_booking_app.databinding.DialogApproveImportOrderBinding
import com.example.movie_booking_app.databinding.DialogRejectImportOrderBinding
import com.example.movie_booking_app.view.adapter.admin.AdminImportOrderAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminImportOrderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminImportOrderFragment : Fragment() {

    private var _binding: ActivityAdminImportOrderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminImportOrderViewModel by viewModels()
    private lateinit var adapter: AdminImportOrderAdapter
    private var originalList: List<ImportOrder> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAdminImportOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide local toolbar
        binding.toolbar.visibility = View.GONE
        (binding.contentLayout.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = 0

        setupRecyclerView()
        setupFilterChips()
        observeViewModel()
        viewModel.loadOrders("PENDING")
    }

    private fun setupRecyclerView() {
        adapter = AdminImportOrderAdapter(
            onApproveClick = { order -> showApproveConfirmDialog(order) },
            onRejectClick  = { order -> showRejectDialog(order) }
        )
        binding.rvImportOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@AdminImportOrderFragment.adapter
        }
    }

    private fun applyFilterAndSearch() {
        if (_binding == null) return
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
        val context = requireContext()
        val activeBg = ContextCompat.getDrawable(context, R.drawable.bg_tab_active_gold)
        val activeTextColor = ContextCompat.getColor(context, R.color.bg_primary)
        val inactiveTextColor = ContextCompat.getColor(context, R.color.text_secondary)

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    originalList = state.orders
                    applyFilterAndSearch()

                    state.successMessageResId?.let { resId ->
                        val msg = if (state.successMessageArg != null) {
                            getString(resId, state.successMessageArg)
                        } else {
                            getString(resId)
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showApproveConfirmDialog(order: ImportOrder) {
        val dialogBinding = DialogApproveImportOrderBinding.inflate(layoutInflater)
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (320 * resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        dialogBinding.tvMessage.text = getString(R.string.goods_intake_approve_msg_template, order.importCode)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            viewModel.approveOrder(order.id)
        }

        dialog.show()
    }

    private fun showRejectDialog(order: ImportOrder) {
        val dialogBinding = DialogRejectImportOrderBinding.inflate(layoutInflater)
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (320 * resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        dialogBinding.tvMessage.text = getString(R.string.goods_intake_reject_msg_template, order.importCode)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            val reason = dialogBinding.etReason.text.toString().trim()
            viewModel.rejectOrder(order.id, reason)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
