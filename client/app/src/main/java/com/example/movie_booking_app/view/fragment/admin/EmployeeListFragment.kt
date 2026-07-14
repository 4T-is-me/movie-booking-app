package com.example.movie_booking_app.view.fragment.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.response.EmployeeItem
import com.example.movie_booking_app.databinding.FragmentAdminUserListBinding
import com.example.movie_booking_app.view.activity.AdminStaffActivity
import com.example.movie_booking_app.view.adapter.AdminStaffAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmployeeListFragment : Fragment() {

    private var _binding: FragmentAdminUserListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminStaffViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: AdminStaffAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data whenever returning to this fragment
        viewModel.loadStaff(binding.edtSearch.text.toString())
    }

    private fun setupRecyclerView() {
        adapter = AdminStaffAdapter(
            onDetailClick = { /* Show Detail */ },
            onEditClick = { staff ->
                val intent = Intent(requireContext(), AdminStaffActivity::class.java).apply {
                    putExtra("staff", staff)
                }
                startActivity(intent)
            },
            onToggleStatusClick = { staff ->
                showConfirmToggleStatusDialog(staff)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun showConfirmToggleStatusDialog(staff: EmployeeItem) {
        val isActive = staff.account?.status != 0
        val title = if (isActive) "Lock Account" else "Unlock Account"
        val message = if (isActive) 
            "Are you sure you want to lock the account for \"${staff.fullName}\"?" 
            else "Are you sure you want to reactivate the account for \"${staff.fullName}\"?"
        val buttonText = if (isActive) "Lock" else "Unlock"
        val newStatus = if (isActive) 0 else 1

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, _ ->
                staff.id?.let { 
                    if (isActive) {
                        viewModel.deleteStaff(it) // Active -> Locked dùng DELETE
                    } else {
                        viewModel.updateStaffStatus(it, 1) // Locked -> Active dùng PATCH status 1
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {
        binding.edtSearch.addTextChangedListener { text ->
            viewModel.loadStaff(text?.toString())
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadStaff(binding.edtSearch.text.toString())
        }
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AdminStaffActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.staffList.observe(viewLifecycleOwner) { list ->
            // Hiển thị toàn bộ danh sách, DiffUtil sẽ tự động cập nhật item thay đổi
            adapter.submitList(ArrayList(list))
            binding.swipeRefresh.isRefreshing = false
        }
        viewModel.isLoadingStaff.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
