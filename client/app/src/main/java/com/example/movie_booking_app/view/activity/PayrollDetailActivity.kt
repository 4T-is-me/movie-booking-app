package com.example.movie_booking_app.view.activity

import android.os.Bundle
import timber.log.Timber
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.PayrollItem
import com.example.movie_booking_app.databinding.ActivityPayrollDetailBinding
import com.example.movie_booking_app.view.adapter.PayrollAdapter
import com.example.movie_booking_app.viewmodel.PayrollViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PayrollDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYROLL_ID = "extra_payroll_id"
    }

    private lateinit var binding: ActivityPayrollDetailBinding
    private val viewModel: PayrollViewModel by viewModels()

    private var currentPayrollId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayrollDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPayrollId = intent.getStringExtra(EXTRA_PAYROLL_ID)

        binding.ivBack.setOnClickListener { finish() }

        if (!currentPayrollId.isNullOrEmpty()) {
            viewModel.loadPayrollDetail(currentPayrollId!!)
        } else {
            Toast.makeText(this, getString(R.string.invalid_payroll_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.payrollDetailState.collect { payroll ->
                        if (payroll != null) {
                            bindPayroll(payroll)
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Toast.makeText(this@PayrollDetailActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.clearActionStates()
                        }
                    }
                }

                launch {
                    viewModel.payResult.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Toast.makeText(this@PayrollDetailActivity, msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearActionStates()
                            // Reload updated data after payment
                            currentPayrollId?.let { viewModel.loadPayrollDetail(it) }
                        }
                    }
                }
            }
        }
    }

    private fun bindPayroll(payroll: PayrollItem) {
        binding.cardEmployeeHeader.visibility = View.VISIBLE
        binding.cardPeriodInfo.visibility = View.VISIBLE
        binding.cardTotalSalary.visibility = View.VISIBLE

        // 1. Employee Header Info
        val name = payroll.employee?.fullName
            ?: payroll.employee?.username
            ?: getString(R.string.user_name_placeholder_employe)

        binding.tvEmployeeName.text = name
        binding.tvEmployeeRole.text = payroll.employee?.role ?: getString(R.string.role_staff)
        binding.tvCinemaName.text = payroll.cinema?.cinemaName ?: getString(R.string.not_available)

        Glide.with(this)
            .load(payroll.employee?.image)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .circleCrop()
            .into(binding.ivAvatar)

        // 2. Status Badge
        val isPaid = payroll.status.equals("Paid", ignoreCase = true)
        if (isPaid) {
            binding.tvStatus.text = getString(R.string.status_paid)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.color_paid_status))
            binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_paid_status_bg)
        } else {
            binding.tvStatus.text = getString(R.string.status_draft)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.color_draft_status))
            binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_draft_status_bg)
        }

        // 3. Period & Total Salary
        binding.tvPeriod.text = getString(R.string.payroll_period_format, payroll.month, payroll.year)
        binding.tvTotalSalary.text = PayrollAdapter.formatVnd(payroll.totalSalary)

        // 4. Role Check (Staff vs Manager)
        val isManager = (payroll.baseSalary ?: 0.0) > 0 ||
                payroll.employee?.role?.contains("manager", ignoreCase = true) == true

        if (isManager) {
            bindManagerDetails(payroll)
        } else {
            bindStaffDetails(payroll)
        }

        // 5. Mark as Paid Button
        val canPay = !isPaid && viewModel.isAdmin()
        binding.btnMarkAsPaid.visibility = if (canPay) View.VISIBLE else View.GONE
        binding.btnMarkAsPaid.setOnClickListener {
            currentPayrollId?.let { id ->
                viewModel.payPayroll(id)
            }
        }
    }

    private fun bindStaffDetails(payroll: PayrollItem) {
        Timber.tag("detail").d(payroll.shiftCount.toString())
        binding.layoutStaffDetails.visibility = View.VISIBLE
        binding.layoutManagerDetails.visibility = View.GONE

        // Shift Salary
        binding.tvShiftSalary.text = PayrollAdapter.formatVnd(payroll.shiftSalary ?: 0.0)

        // Shift Count Details
        val sc = payroll.shiftCount
        binding.tvMorningCount.text = formatShiftCount(sc?.morning ?: 0)
        binding.tvAfternoonCount.text = formatShiftCount(sc?.afternoon ?: 0)
        binding.tvEveningCount.text = formatShiftCount(sc?.evening ?: 0)
        binding.tvNightCount.text = formatShiftCount(sc?.night ?: 0)
        binding.tvAbsentCount.text = formatShiftCount(sc?.absent ?: 0)

        // Penalty & Bonus
        binding.tvPenalty.text = getString(R.string.minus_amount_format, PayrollAdapter.formatVnd(payroll.penalty ?: 0.0))
        binding.tvBonus.text = getString(R.string.plus_amount_format, PayrollAdapter.formatVnd(payroll.bonus ?: 0.0))
    }

    private fun bindManagerDetails(payroll: PayrollItem) {
        binding.layoutManagerDetails.visibility = View.VISIBLE
        binding.layoutStaffDetails.visibility = View.GONE

        // Base Salary & Revenue Bonus
        binding.tvBaseSalary.text = PayrollAdapter.formatVnd(payroll.baseSalary ?: 0.0)
        binding.tvRevenueBonus.text = getString(R.string.plus_amount_format, PayrollAdapter.formatVnd(payroll.bonus ?: 0.0))
    }

    // Helper xử lý số nhiều (1 shift / 2 shifts)
    private fun formatShiftCount(count: Int): String {
        return if (count == 1) {
            getString(R.string.shift_count_singular, count)
        } else {
            getString(R.string.shift_count_plural, count)
        }
    }
}