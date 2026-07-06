package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.FragmentAdminStaffBinding
import com.example.movie_booking_app.view.adapter.AdminStaffAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminStaffFragment : Fragment() {

    private var _binding: FragmentAdminStaffBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminStaffViewModel by viewModels()
    private lateinit var adapter: AdminStaffAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AdminStaffAdapter()
        binding.rvAdminStaff.layoutManager = LinearLayoutManager(context)
        binding.rvAdminStaff.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchStaff.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null })
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Show loading state if needed, here we submit the list
                adapter.submitList(state.staffList)
                
                state.error?.let { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
