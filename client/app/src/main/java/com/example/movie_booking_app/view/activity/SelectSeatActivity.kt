package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivitySelectSeatBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.viewmodel.SelectSeatViewModel
import kotlinx.coroutines.launch

class SelectSeatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectSeatBinding
    private lateinit var viewModel: SelectSeatViewModel

    private lateinit var seatAdapter: SeatAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SelectSeatViewModel::class.java]

        setupUI()
        setupAdapters()
        observeViewModel()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }

        // REQUIRED BY TASK: Spinner chọn số lượng ghế (1 đến 8)
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

        // Setup Legend
        binding.legendAvailable.viewSwatch.setBackgroundResource(R.drawable.bg_seat_available)
        binding.legendAvailable.tvLegendLabel.text = getString(R.string.legend_available)

        binding.legendVip.viewSwatch.setBackgroundResource(R.drawable.bg_seat_vip)
        binding.legendVip.tvLegendLabel.text = getString(R.string.legend_vip)

        binding.legendSold.viewSwatch.setBackgroundResource(R.drawable.bg_seat_sold)
        binding.legendSold.tvLegendLabel.text = getString(R.string.legend_sold)

        binding.legendSelected.viewSwatch.setBackgroundResource(R.drawable.bg_seat_selected)
        binding.legendSelected.tvLegendLabel.text = getString(R.string.legend_selected)

        binding.btnContinue.setOnClickListener {
            viewModel.validateAndProceed()
        }
    }

    private fun setupAdapters() {
        seatAdapter = SeatAdapter { seat -> viewModel.onSeatClicked(seat) }
        binding.rvSeats.layoutManager = GridLayoutManager(this, 10)
        binding.rvSeats.adapter = seatAdapter

        dateAdapter = DateAdapter { date -> /* Tương lai: Gọi API lấy suất chiếu theo ngày */ }
        binding.rvDates.adapter = dateAdapter

        timeAdapter = TimeAdapter { time -> /* Tương lai: Gọi API lấy ghế theo giờ */ }
        binding.rvTimes.adapter = timeAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.seats.collect { seats ->
                seatAdapter.submitList(seats)
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

        // REQUIRED BY TASK: Xử lý logic validate số lượng ghế
        lifecycleScope.launch {
            viewModel.validationError.collect { error ->
                when (error) {
                    is SelectSeatViewModel.ValidationError.SelectMore -> {
                        val msg = getString(R.string.error_select_more, error.count)
                        Toast.makeText(this@SelectSeatActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    is SelectSeatViewModel.ValidationError.SelectOnly -> {
                        val msg = getString(R.string.error_select_only, error.count)
                        Toast.makeText(this@SelectSeatActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    is SelectSeatViewModel.ValidationError.Success -> {
                        Toast.makeText(this@SelectSeatActivity, "Proceeding to checkout...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
