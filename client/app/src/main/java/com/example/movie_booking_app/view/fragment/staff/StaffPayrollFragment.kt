package com.example.movie_booking_app.view.fragment.staff

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.FragmentPayrollListBinding
import com.example.movie_booking_app.view.activity.PayrollDetailActivity
import com.example.movie_booking_app.view.adapter.PayrollAdapter
import com.example.movie_booking_app.viewmodel.staff.StaffPayrollViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffPayrollFragment : Fragment() {

    private var _binding: FragmentPayrollListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StaffPayrollViewModel by viewModels()
    private lateinit var payrollAdapter: PayrollAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayrollListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        viewModel.loadStaffPayrolls()

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.payrollListState.collect { list ->
                        payrollAdapter.submitList(list)
                        binding.layoutEmptyState.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvEmptyMessage.text = getString(R.string.auto_str_no_payroll_found)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
