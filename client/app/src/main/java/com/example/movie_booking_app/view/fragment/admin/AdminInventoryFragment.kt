package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityAdminInventoryBinding
import com.example.movie_booking_app.utils.Constants
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
        inventoryAdapter = InventoryAdapter()

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
                        val names = state.cinemas.map { it.cinemaName ?: Constants.UNKNOWN }
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
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
