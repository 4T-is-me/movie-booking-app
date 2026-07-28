package com.example.movie_booking_app.view.fragment.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.databinding.FragmentAdminPayrollBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.activity.PayrollDetailActivity
import com.example.movie_booking_app.view.activity.PayrollConfigActivity
import com.example.movie_booking_app.view.adapter.PayrollAdapter
import com.example.movie_booking_app.view.fragment.GeneratePayrollDialogFragment
import com.example.movie_booking_app.viewmodel.PayrollViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AdminPayrollFragment : Fragment() {

    private var _binding: FragmentAdminPayrollBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PayrollViewModel by viewModels()
    private lateinit var payrollAdapter: PayrollAdapter

    // Filter state
    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null
    private var selectedCinemaId: String? = null
    private var selectedStatus: String? = null
    private var cinemaList: List<Cinema> = emptyList()

    private var spinnerMonthReady = false
    private var spinnerYearReady = false
    private var spinnerCinemaReady = false
    private var spinnerStatusReady = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPayrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupStaticSpinners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        payrollAdapter = PayrollAdapter { payrollItem ->
            val intent = Intent(requireContext(), PayrollDetailActivity::class.java).apply {
                putExtra(PayrollDetailActivity.EXTRA_PAYROLL_ID, payrollItem.id)
            }
            startActivity(intent)
        }
        binding.rvPayrolls.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = payrollAdapter
        }
    }

    private fun setupStaticSpinners() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Month spinner
        val months = listOf(getString(R.string.all_label)) + (1..12).map { getString(R.string.month_format, it) }
        setupSpinner(binding.spinnerMonth, months) { position ->
            if (!spinnerMonthReady) { spinnerMonthReady = true; return@setupSpinner }
            selectedMonth = if (position == 0) null else position
            applyFilters()
        }
        // Default to current month
        binding.spinnerMonth.setSelection(currentMonth)
        selectedMonth = currentMonth

        // Year spinner
        val years = listOf(getString(R.string.all_label)) + (currentYear - 2..currentYear + 1).map { it.toString() }
        setupSpinner(binding.spinnerYear, years) { position ->
            if (!spinnerYearReady) { spinnerYearReady = true; return@setupSpinner }
            selectedYear = if (position == 0) null else years[position].toIntOrNull()
            applyFilters()
        }
        val currentYearIndex = years.indexOf(currentYear.toString())
        if (currentYearIndex >= 0) binding.spinnerYear.setSelection(currentYearIndex)
        selectedYear = currentYear

        // Status spinner
        val statuses = listOf(getString(R.string.all_status), getString(R.string.status_draft), getString(R.string.status_paid))
        setupSpinner(binding.spinnerStatus, statuses) { position ->
            if (!spinnerStatusReady) { spinnerStatusReady = true; return@setupSpinner }
            selectedStatus = if (position == 0) null else {
                if (position == 1) "Draft" else "Paid" // Still need the API-friendly string for the ViewModel
            }
            applyFilters()
        }
    }

    private fun setupCinemaSpinner(cinemas: List<Cinema>) {
        cinemaList = cinemas
        val cinemaNames = listOf(getString(R.string.all_cinemas)) + cinemas.map { it.cinemaName ?: getString(R.string.unknown) }
        setupSpinner(binding.spinnerCinema, cinemaNames) { position ->
            if (!spinnerCinemaReady) { spinnerCinemaReady = true; return@setupSpinner }
            selectedCinemaId = if (position == 0) null else cinemas.getOrNull(position - 1)?.id
            applyFilters()
        }
    }

    private fun setupManagerCinemaSpinner(cinemaName: String) {
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, listOf(cinemaName))
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerCinema.adapter = adapter
        binding.spinnerCinema.isEnabled = false
    }

    private fun setupSpinner(
        spinner: android.widget.Spinner,
        items: List<String>,
        onSelected: (position: Int) -> Unit
    ) {
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, items)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        viewModel.loadPayrolls(selectedMonth, selectedYear, selectedCinemaId, selectedStatus)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Role-based UI setup
                if (viewModel.isAdmin()) {
                    binding.btnGeneratePayroll.visibility = View.VISIBLE
                    binding.btnGeneratePayroll.setOnClickListener { showGenerateDialog() }
                    binding.btnPayrollConfig.visibility = View.VISIBLE
                    binding.btnPayrollConfig.setOnClickListener {
                        startActivity(Intent(requireContext(), PayrollConfigActivity::class.java).putExtra(PayrollConfigActivity.EXTRA_IS_ADMIN, true))
                    }
                } else {
                    binding.btnGeneratePayroll.visibility = View.GONE
                    binding.btnPayrollConfig.visibility = View.GONE
                }

                launch {
                    viewModel.payrollListState.collect { list ->
                        payrollAdapter.submitList(list)
                        binding.layoutEmptyState.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.cinemasState.collect { cinemas ->
                        if (cinemas.isNotEmpty()) {
                            if (viewModel.isAdmin()) {
                                setupCinemaSpinner(cinemas)
                            }
                        }
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
                            viewModel.clearActionStates()
                        }
                    }
                }

                launch {
                    viewModel.generateResult.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearActionStates()
                            applyFilters()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect {
                    val cinemaId = viewModel.userCinemaId
                    if (viewModel.isManager() && cinemaId != null) {
                        setupManagerCinemaSpinner(getString(R.string.my_cinema))
                    }
                }
            }
        }
    }

    private fun showGenerateDialog() {
        val dialog = GeneratePayrollDialogFragment { month, year ->
            viewModel.generatePayroll(month, year)
        }
        dialog.show(parentFragmentManager, "GeneratePayrollDialog")
    }

    override fun onResume() {
        super.onResume()
        applyFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}