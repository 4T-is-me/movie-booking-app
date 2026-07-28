package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityPayrollConfigBinding
import com.example.movie_booking_app.viewmodel.PayrollConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PayrollConfigActivity : AppCompatActivity() {
    companion object { const val EXTRA_IS_ADMIN = "extra_is_admin" }
    private lateinit var binding: ActivityPayrollConfigBinding
    private val viewModel: PayrollConfigViewModel by viewModels()
    private val isAdmin by lazy { intent.getBooleanExtra(EXTRA_IS_ADMIN, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayrollConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvReadOnly.visibility = if (isAdmin) View.GONE else View.VISIBLE
        binding.btnSaveConfig.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.ivEditIndicator.visibility = if (isAdmin) View.VISIBLE else View.GONE
        setPayrollConfigEditable(isAdmin)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveConfig.setOnClickListener { updatePayrollConfig() }
        observePayrollConfigUi()
        viewModel.loadPayrollConfig()
    }

    private fun setPayrollConfigEditable(editable: Boolean) = listOf(binding.etMorningRate, binding.etAfternoonRate, binding.etEveningRate, binding.etNightRate, binding.etHolidayMultiplier, binding.etManagerRevenueBonus).forEach {
        it.isEnabled = editable
        it.alpha = if (editable) 1f else 0.85f
    }

    private fun observePayrollConfigUi() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.config.collect { config -> config?.let { bindPayrollConfig(it.shiftRates, it.holidayMultiplier, it.managerRevenueBonus) } } }
            launch { viewModel.loading.collect { binding.progressConfig.visibility = if (it) View.VISIBLE else View.GONE } }
            launch { viewModel.message.collect { message -> message?.let { Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show(); viewModel.clearMessage() } } }
            launch {
                viewModel.updateSuccess.collect { success ->
                    if (success) {
                        finish()
                    }
                }
            }
        }
    }

    private fun bindPayrollConfig(rates: Map<String, Double>, multiplier: Double, bonus: Double) {
        binding.etMorningRate.setText(rates["Morning"]?.format() ?: "")
        binding.etAfternoonRate.setText(rates["Afternoon"]?.format() ?: "")
        binding.etEveningRate.setText(rates["Evening"]?.format() ?: "")
        binding.etNightRate.setText(rates["Night"]?.format() ?: "")
        binding.etHolidayMultiplier.setText(multiplier.format())
        binding.etManagerRevenueBonus.setText(bonus.format())
    }

    private fun updatePayrollConfig() {
        val values = listOf(binding.etMorningRate, binding.etAfternoonRate, binding.etEveningRate, binding.etNightRate, binding.etHolidayMultiplier, binding.etManagerRevenueBonus).map { it.text.toString().toDoubleOrNull() }
        if (values.any { it == null || it < 0 } || values[4]!! < 1) { Toast.makeText(this, R.string.invalid_payroll_config, Toast.LENGTH_LONG).show(); return }
        viewModel.updatePayrollConfig(mapOf("Morning" to values[0]!!, "Afternoon" to values[1]!!, "Evening" to values[2]!!, "Night" to values[3]!!), values[4]!!, values[5]!!)
    }

    private fun Double.format() = if (this % 1.0 == 0.0) toLong().toString() else toString()
}
