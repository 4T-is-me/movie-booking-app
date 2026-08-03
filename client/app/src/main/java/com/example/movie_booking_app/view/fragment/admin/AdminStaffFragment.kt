package com.example.movie_booking_app.view.fragment.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.databinding.DialogConfirmDeleteStaffBinding
import com.example.movie_booking_app.databinding.DialogDetailStaffBinding
import com.example.movie_booking_app.databinding.FragmentAdminStaffBinding
import com.example.movie_booking_app.view.activity.AdminStaffActivity
import com.example.movie_booking_app.view.adapter.AdminStaffAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        private const val ARG_ROLE = "arg_role"

        fun newInstance(role: String): AdminStaffFragment {
            val fragment = AdminStaffFragment()
            val args = Bundle()
            args.putString(ARG_ROLE, role)
            fragment.arguments = args
            return fragment
        }
    }

    private var currentRole: String = "Manager"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        currentRole = arguments?.getString(ARG_ROLE) ?: "Manager"
        viewModel.setRole(currentRole)

        setupRecyclerView()
        setupSearch()
        setupTabs()
        observeViewModel()

        binding.swipeRefreshLayout.setOnRefreshListener {
            val query = binding.etSearchStaff.text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null })
        }

        binding.fabAddStaff.setOnClickListener {
            val intent = Intent(requireContext(), AdminStaffActivity::class.java).apply {
                putExtra("role", currentRole)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val query = binding.etSearchStaff.text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null }, showLoading = false)
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
            onDeleteClick = { staff ->
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

    private fun showDeleteConfirmationDialog(staff: Staff) {
        val dialogBinding = DialogConfirmDeleteStaffBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        dialogBinding.tvDeleteTitle.text = getString(R.string.auto_str_delete_account)
        dialogBinding.tvDeleteMessage.text = getString(R.string.admin_confirm_delete_staff_msg, currentRole, staff.fullName ?: "")

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            dialog.dismiss()
            staff.id?.let { viewModel.deleteStaff(it) }
        }

        dialog.show()
    }

    private fun showStaffDetailDialog(staff: Staff) {
        val dialogBinding = DialogDetailStaffBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        dialogBinding.tvDetailUsername.text = staff.username ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailFullName.text = staff.fullName ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailPosition.text = staff.position ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailEmail.text = staff.email ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailPhone.text = staff.phone ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailDob.text = staff.dob?.take(10) ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailGender.text = staff.gender ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailIdentityCard.text = staff.identityCard ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailAddress.text = staff.address ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailSalary.text = staff.salary?.let { String.format(Locale.getDefault(), getString(R.string.admin_currency_format), it) } ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailStartDate.text = staff.startDate?.take(10) ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailKpi.text = staff.kpiPoints?.toString() ?: "0"
        dialogBinding.tvDetailTicketSold.text = staff.ticketSold?.toString() ?: "0"
        dialogBinding.tvDetailRating.text = staff.rating?.toString() ?: "0.0"

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.swipeRefreshLayout.isRefreshing = state.isLoading
                // Show loading state if needed, here we submit the list
                adapter.submitList(getFilteredStaffList(state.staffList))
                
                state.error?.let { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionSuccess.collectLatest { success ->
                if (success) {
                    viewModel.loadStaff()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionMessage.collectLatest { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
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
        
        val fullList = viewModel.uiState.value.staffList
        adapter.submitList(getFilteredStaffList(fullList))
    }

    private fun updateTabStyle(tabView: TextView, isActive: Boolean) {
        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_tab_active)
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
        }
    }

    private fun getFilteredStaffList(fullList: List<Staff>): List<Staff> {
        return if (selectedTab == TAB_ACTIVE) {
            fullList.filter { it.status != 0 }
        } else {
            fullList.filter { it.status == 0 }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
