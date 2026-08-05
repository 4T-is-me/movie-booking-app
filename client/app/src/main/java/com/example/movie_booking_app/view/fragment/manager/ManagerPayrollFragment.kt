package com.example.movie_booking_app.view.fragment.manager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.PayrollItem
import com.example.movie_booking_app.databinding.FragmentManagerPayrollBinding
import com.example.movie_booking_app.utils.NameFormatter
import com.example.movie_booking_app.view.activity.PayrollConfigActivity
import com.example.movie_booking_app.view.activity.PayrollDetailActivity
import com.example.movie_booking_app.view.adapter.PayrollAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerPayrollViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerPayrollFragment : Fragment() {

    private var _binding: FragmentManagerPayrollBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManagerPayrollViewModel by viewModels()
    private lateinit var staffAdapter: PayrollAdapter

    // Keep track of data to filter locally
    private var allStaffPayrolls: List<PayrollItem> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerPayrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        binding.btnPayrollConfig.setOnClickListener {
            startActivity(Intent(requireContext(), PayrollConfigActivity::class.java).putExtra(PayrollConfigActivity.EXTRA_IS_ADMIN, false))
        }
        observeViewModel()
        viewModel.loadManagerPayrolls()
    }

    private fun setupRecyclerView() {
        staffAdapter = PayrollAdapter { payrollItem ->
            openPayrollDetail(payrollItem.id)
        }
        binding.rvStaffPayrolls.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffAdapter
        }
    }

    private fun setupFilters() {
        // Setup Search
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                filterStaffList()
                return true
            }
        })

        // Setup Spinner status
        val statusOptions = listOf("All", "Draft", "Paid")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, statusOptions)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerStatusFilter.adapter = spinnerAdapter
        binding.spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStatusFilter = statusOptions[position]
                filterStaffList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterStaffList() {
        val filtered = allStaffPayrolls.filter { item ->
            val name = item.employee?.fullName ?: item.employee?.username ?: ""
            val matchesSearch = name.contains(currentSearchQuery, ignoreCase = true)
            val matchesStatus = if (currentStatusFilter == "All") {
                true
            } else {
                item.status.equals(currentStatusFilter, ignoreCase = true)
            }
            matchesSearch && matchesStatus
        }
        staffAdapter.submitList(filtered)
        binding.layoutEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.payrollListState.collect { list ->
                        // 1. Separate Manager and Staff Data
                        val managerPayroll = list.find {
                            it.employee?.role?.contains("manager", ignoreCase = true) == true
                        }
                        allStaffPayrolls = list.filter {
                            it.employee?.role?.contains("manager", ignoreCase = true) == false
                        }

                        // 2. Bind Manager Card directly
                        if (managerPayroll != null) {
                            binding.cardManager.visibility = View.VISIBLE
                            bindManagerCard(managerPayroll)
                        } else {
                            binding.cardManager.visibility = View.GONE
                        }

                        // 3. Bind Staff RecyclerView with filters
                        filterStaffList()
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                        if (loading) binding.layoutEmptyState.visibility = View.GONE
                    }
                }

                launch {
                    viewModel.errorMessage.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun bindManagerCard(managerPayroll: PayrollItem) {
        val ctx = requireContext()
        val name = managerPayroll.employee?.fullName ?: managerPayroll.employee?.username ?: ""
        binding.tvManagerName.text = name
        binding.tvManagerAvatarText.text = NameFormatter.getAvatarInitials(name)
        binding.tvManagerCinemaName.text = managerPayroll.cinema?.cinemaName ?: getString(R.string.lumina_cinema)
        binding.tvManagerBaseSalary.text = PayrollAdapter.formatVnd(managerPayroll.baseSalary ?: 0.0)
        binding.tvManagerBonus.text = getString(R.string.auto_str_str_auto) + PayrollAdapter.formatVnd(managerPayroll.bonus ?: 0.0)
        binding.tvManagerTotalSalary.text = PayrollAdapter.formatVnd(managerPayroll.totalSalary)

        // Status Badge Style
        val isDraft = managerPayroll.status.equals("Draft", ignoreCase = true)
        if (isDraft) {
            binding.tvManagerStatus.text = ctx.getString(R.string.status_draft)
            binding.tvManagerStatus.setTextColor(ctx.getColor(R.color.color_draft_status))
            binding.tvManagerStatus.backgroundTintList = ctx.getColorStateList(R.color.color_draft_status_bg)
        } else {
            binding.tvManagerStatus.text = ctx.getString(R.string.status_paid)
            binding.tvManagerStatus.setTextColor(ctx.getColor(R.color.color_paid_status))
            binding.tvManagerStatus.backgroundTintList = ctx.getColorStateList(R.color.color_paid_status_bg)
        }

        binding.cardManager.setOnClickListener {
            openPayrollDetail(managerPayroll.id)
        }
    }

    private fun openPayrollDetail(payrollId: String) {
        val intent = Intent(requireContext(), PayrollDetailActivity::class.java).apply {
            putExtra(PayrollDetailActivity.EXTRA_PAYROLL_ID, payrollId)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
