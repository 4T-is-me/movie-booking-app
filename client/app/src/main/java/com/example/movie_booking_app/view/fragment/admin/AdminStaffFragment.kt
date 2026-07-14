package com.example.movie_booking_app.view.fragment.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.response.EmployeeItem
import com.example.movie_booking_app.databinding.FragmentAdminStaffBinding
import com.example.movie_booking_app.view.activity.AdminStaffActivity
import com.example.movie_booking_app.view.adapter.AdminStaffAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminStaffFragment : Fragment() {

    private var _binding: FragmentAdminStaffBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminStaffViewModel by viewModels()
    private lateinit var adapter: AdminStaffAdapter
    
    private var selectedTab = TAB_ACTIVE

    companion object {
        private const val TAB_ACTIVE = "active"
        private const val TAB_INACTIVE = "inactive"
    }

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
        setupTabs()
        observeViewModel()

        binding.swipeRefreshLayout.setOnRefreshListener {
            val query = binding.etSearchStaff.text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null })
        }

        binding.fabAddStaff.setOnClickListener {
            val intent = Intent(requireContext(), AdminStaffActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminStaffAdapter(
            onDetailClick = { staff ->
                showStaffDetailDialog(staff)
            },
            onEditClick = { staff ->
                val intent = Intent(requireContext(), AdminStaffActivity::class.java).apply {
                    putExtra("staff", staff)
                }
                startActivity(intent)
            },
            onToggleStatusClick = { staff ->
                showDeleteConfirmationDialog(staff)
            }
        )
        binding.rvAdminStaff.layoutManager = LinearLayoutManager(context)
        binding.rvAdminStaff.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchStaff.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null })
        }
    }

    private fun showDeleteConfirmationDialog(staff: EmployeeItem) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_confirm_delete_staff)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDeleteMessage = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDeleteMessage)
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnCancelDelete)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnConfirmDelete)

        tvDeleteMessage.text = "Are you sure you want to delete employee \"${staff.fullName}\"?"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            staff.id?.let { viewModel.deleteStaff(it) }
        }

        dialog.show()
    }

    private fun showStaffDetailDialog(staff: EmployeeItem) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_detail_staff)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDetailUsername = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailUsername)
        val tvDetailFullName = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailFullName)
        val tvDetailPosition = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailPosition)
        val tvDetailEmail = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailEmail)
        val tvDetailPhone = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailPhone)
        val tvDetailDob = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailDob)
        val tvDetailGender = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailGender)
        val tvDetailIdentityCard = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailIdentityCard)
        val tvDetailAddress = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailAddress)
        val tvDetailSalary = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailSalary)
        val tvDetailStartDate = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailStartDate)
        val tvDetailKpi = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailKpi)
        val tvDetailTicketSold = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailTicketSold)
        val tvDetailRating = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDetailRating)
        val btnClose = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnCloseDetail)

        tvDetailUsername.text = staff.account?.username ?: "-"
        tvDetailFullName.text = staff.fullName ?: "-"
        tvDetailPosition.text = staff.position ?: "-"
        tvDetailEmail.text = staff.account?.email ?: "-"
        tvDetailPhone.text = staff.phone ?: "-"
        tvDetailDob.text = "-" // EmployeeItem doesn't have dob in response yet
        tvDetailGender.text = "-" // EmployeeItem doesn't have gender in response yet
        tvDetailIdentityCard.text = staff.identityCard ?: "-"
        tvDetailAddress.text = staff.address ?: "-"
        tvDetailSalary.text = staff.salary?.let { String.format("%,.0f đ", it) } ?: "-"
        tvDetailStartDate.text = staff.startDate?.take(10) ?: "-"
        tvDetailKpi.text = staff.kpiPoints?.toString() ?: "0"
        tvDetailTicketSold.text = staff.ticketSold?.toString() ?: "0"
        tvDetailRating.text = staff.rating?.toString() ?: "0.0"

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.isLoadingStaff.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.staffList.observe(viewLifecycleOwner) { staffList ->
            // Since adapter expects Staff, we might need to map EmployeeItem back to Staff 
            // or update adapter to use EmployeeItem. 
            // For now, let's update adapter to use EmployeeItem to follow the plan.
            adapter.submitList(getFilteredStaffList(staffList))
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                viewModel.loadStaff()
            }
        }

        viewModel.actionMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        binding.tabActiveStaff.setOnClickListener { switchTab(TAB_ACTIVE) }
        binding.tabInactiveStaff.setOnClickListener { switchTab(TAB_INACTIVE) }
    }

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab
        
        val isActive = (tab == TAB_ACTIVE)
        updateTabStyle(binding.tabActiveStaff, isActive)
        updateTabStyle(binding.tabInactiveStaff, !isActive)
        
        val fullList = viewModel.staffList.value ?: emptyList()
        adapter.submitList(getFilteredStaffList(fullList))
    }

    private fun updateTabStyle(tabView: android.widget.TextView, isActive: Boolean) {
        if (isActive) {
            tabView.setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_tab_active)
            tabView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.movie_booking_app.R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.movie_booking_app.R.color.text_gray))
        }
    }

    private fun getFilteredStaffList(fullList: List<EmployeeItem>): List<EmployeeItem> {
        return if (selectedTab == TAB_ACTIVE) {
            fullList.filter { it.account?.status != 0 }
        } else {
            fullList.filter { it.account?.status == 0 }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
