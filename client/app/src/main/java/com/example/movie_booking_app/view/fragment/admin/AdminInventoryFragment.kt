package com.example.movie_booking_app.view.fragment.admin

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.databinding.ActivityAdminInventoryBinding
import com.example.movie_booking_app.view.adapter.admin.InventoryAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminInventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminInventoryFragment : Fragment() {

    private var _binding: ActivityAdminInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminInventoryViewModel by viewModels()
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAdminInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide local toolbar
        binding.toolbar.visibility = View.GONE
        
        // Update constraints: layoutCinemaSelector was below toolbar
        val params = binding.layoutCinemaSelector.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.topToBottom = -1
        binding.layoutCinemaSelector.layoutParams = params

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        inventoryAdapter = InventoryAdapter { item ->
            showRefillDialog(item)
        }

        binding.rvInventory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inventoryAdapter
        }

        binding.actvCinema.setOnItemClickListener { _, _, position, _ ->
            val cinema = viewModel.uiState.value.cinemas.getOrNull(position)
            cinema?.let { viewModel.selectCinema(it) }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    if (state.cinemas.isNotEmpty() && binding.actvCinema.adapter == null) {
                        val names = state.cinemas.map { it.cinemaName ?: "Unknown" }
                        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_dropdown, names)
                        binding.actvCinema.setAdapter(adapter)
                        
                        if (names.isNotEmpty()) {
                            binding.actvCinema.setText(names[0], false)
                            viewModel.selectCinema(state.cinemas[0])
                        }
                    }

                    inventoryAdapter.submitList(state.inventoryItems)

                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }

                    state.refillSuccess?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showRefillDialog(item: Inventory) {
        val context = requireContext()
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
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
