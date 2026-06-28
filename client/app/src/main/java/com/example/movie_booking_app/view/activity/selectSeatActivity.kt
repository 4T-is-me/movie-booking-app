package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivitySelectSeatBinding
import com.example.movie_booking_app.view.adapter.dateAdapter
import com.example.movie_booking_app.view.adapter.seatAdapter
import com.example.movie_booking_app.view.adapter.timeAdapter
import com.example.movie_booking_app.viewmodel.selectSeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class selectSeatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectSeatBinding
    private val viewModel: selectSeatViewModel by viewModels()

    private lateinit var seatAdapter: seatAdapter
    private lateinit var dateAdapter: dateAdapter
    private lateinit var timeAdapter: timeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupUI()
        setupAdapters()
        observeViewModel()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top)
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutFooter) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = insets.bottom)
            windowInsets
        }
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }

        val seatCounts = (1..8).toList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seatCounts)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuantity.adapter = spinnerAdapter
        
        binding.spinnerQuantity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setTargetCount(seatCounts[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnContinue.setOnClickListener {
            viewModel.validateAndProceed()
        }

        binding.legendAvailable.apply {
            viewSwatch.setBackgroundResource(R.drawable.bg_seat_available)
            tvLegendLabel.text = getString(R.string.legend_available)
        }
        binding.legendVip.apply {
            viewSwatch.setBackgroundResource(R.drawable.bg_seat_vip)
            tvLegendLabel.text = getString(R.string.legend_vip)
        }
        binding.legendSold.apply {
            viewSwatch.setBackgroundResource(R.drawable.bg_seat_sold)
            tvLegendLabel.text = getString(R.string.legend_sold)
        }
        binding.legendSelected.apply {
            viewSwatch.setBackgroundResource(R.drawable.bg_seat_selected)
            tvLegendLabel.text = getString(R.string.legend_selected)
        }
        
        updateContinueButtonState(false)
    }

    private fun updateContinueButtonState(isEnabled: Boolean) {
        binding.btnContinue.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnContinue.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent_primary)
            binding.btnContinue.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            binding.btnContinue.backgroundTintList = ContextCompat.getColorStateList(this, R.color.seat_available)
            binding.btnContinue.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        }
    }

    private fun setupAdapters() {
        seatAdapter = seatAdapter { seat -> viewModel.onSeatClicked(seat) }
        binding.rvSeats.layoutManager = GridLayoutManager(this, 10)
        binding.rvSeats.adapter = seatAdapter

        dateAdapter = dateAdapter { date -> viewModel.onDateSelected(date) }
        binding.rvDates.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDates.adapter = dateAdapter

        timeAdapter = timeAdapter { time -> viewModel.onTimeSelected(time) }
        binding.rvTimes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTimes.adapter = timeAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.layoutSkeleton.root.visibility = View.VISIBLE
                    binding.mainContent.visibility = View.GONE
                    binding.btnContinue.visibility = View.INVISIBLE
                } else {
                    binding.layoutSkeleton.root.visibility = View.GONE
                    binding.mainContent.visibility = View.VISIBLE
                    binding.btnContinue.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.seats.collect { seats ->
                seatAdapter.submitList(seats)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedSeats.collect { selected ->
                updateContinueButtonState(selected.isNotEmpty())
            }
        }

        lifecycleScope.launch {
            viewModel.showDates.collect { dates ->
                dateAdapter.submitList(dates)
            }
        }

        lifecycleScope.launch {
            viewModel.showTimes.collect { times ->
                timeAdapter.submitList(times)
            }
        }

        lifecycleScope.launch {
            viewModel.totalAmount.collect { amount ->
                binding.tvTotalAmount.text = String.format("%,d VND", amount)
            }
        }

        lifecycleScope.launch {
            viewModel.validationError.collect { error ->
                when (error) {
                    is selectSeatViewModel.ValidationError.SelectMore -> {
                        Toast.makeText(this@selectSeatActivity, getString(R.string.error_select_more, error.count), Toast.LENGTH_SHORT).show()
                    }
                    is selectSeatViewModel.ValidationError.SelectOnly -> {
                        Toast.makeText(this@selectSeatActivity, getString(R.string.error_select_only, error.count), Toast.LENGTH_SHORT).show()
                    }
                    is selectSeatViewModel.ValidationError.Success -> {
                        Toast.makeText(this@selectSeatActivity, getString(R.string.msg_proceeding), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
