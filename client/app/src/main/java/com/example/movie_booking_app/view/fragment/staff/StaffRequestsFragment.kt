package com.example.movie_booking_app.view.fragment.staff

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.FragmentStaffRequestsBinding
import com.example.movie_booking_app.view.adapter.MyLeaveRequestsAdapter
import com.example.movie_booking_app.viewmodel.staff.LeaveRequestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
@AndroidEntryPoint
class StaffRequestsFragment : Fragment() {

    private var _binding: FragmentStaffRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaveRequestViewModel by viewModels()
    private lateinit var leaveRequestsAdapter: MyLeaveRequestsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadMyLeaveRequests()
    }

    private fun setupRecyclerView() {
        leaveRequestsAdapter = MyLeaveRequestsAdapter(
            onEditClick = { item ->
                Toast.makeText(requireContext(), "Edit requested for ID: ${item.leaveRequestId}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { item ->
                Toast.makeText(requireContext(), "Delete requested for ID: ${item.leaveRequestId}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvLeaveRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaveRequestsAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.leaveRequests.collectLatest { list ->
                Log.d("list",list.toString() )
                leaveRequestsAdapter.submitList(list)
                binding.txtNoData.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                if (message != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
