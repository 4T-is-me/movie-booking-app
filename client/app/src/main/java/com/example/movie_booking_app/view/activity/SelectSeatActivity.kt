package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivitySelectSeatBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.viewmodel.SelectSeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * Activity for Cinema Seat Selection screen.
 */
@AndroidEntryPoint
class SelectSeatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectSeatBinding
    private val viewModel: SelectSeatViewModel by viewModels()

    private lateinit var seatAdapter: SeatAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupLegend()
        setupQuantitySpinner()
        setupListeners()
        observeViewModel()
    }

    private fun setupLegend() {
        binding.legendAvailable.tvLegendLabel.text = getString(R.string.legend_available)
        binding.legendAvailable.viewSwatch.setBackgroundResource(R.drawable.bg_seat_available)

        binding.legendVip.tvLegendLabel.text = getString(R.string.legend_vip)
        binding.legendVip.viewSwatch.setBackgroundResource(R.drawable.bg_seat_vip)

        binding.legendCouple.tvLegendLabel.text = getString(R.string.legend_couple)
        binding.legendCouple.viewSwatch.setBackgroundResource(R.drawable.bg_seat_couple)

        binding.legendSold.tvLegendLabel.text = getString(R.string.legend_sold)
        binding.legendSold.viewSwatch.setBackgroundResource(R.drawable.bg_seat_sold)

        binding.legendSelected.tvLegendLabel.text = getString(R.string.legend_selected)
        binding.legendSelected.viewSwatch.setBackgroundResource(R.drawable.bg_seat_selected)
    }

    private fun setupRecyclerViews() {
        seatAdapter = SeatAdapter { seat ->
            viewModel.toggleSeatSelection(seat.id)
        }
        binding.rvSeats.layoutManager = GridLayoutManager(this, 10)
        binding.rvSeats.adapter = seatAdapter
        binding.rvSeats.isNestedScrollingEnabled = false

        dateAdapter = DateAdapter { date ->
            viewModel.selectDate(date.id)
        }
        binding.rvDates.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDates.adapter = dateAdapter

        timeAdapter = TimeAdapter { showtime  ->
            viewModel.selectTime(showtime.id)
        }
        binding.rvTimes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTimes.adapter = timeAdapter
    }

    private fun setupQuantitySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, (1..8).toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuantity.adapter = adapter
        binding.spinnerQuantity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setTargetCount(position + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        
        binding.btnContinue.setOnClickListener {
            val selectedSeatsList = viewModel.selectedSeats.value
            val targetCount = viewModel.targetCount.value
            
            if (selectedSeatsList.size < targetCount) {
                val diff = targetCount - selectedSeatsList.size
                Toast.makeText(this, getString(R.string.error_select_more, diff), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedSeatsList.size > targetCount) {
                Toast.makeText(this, getString(R.string.error_select_only, targetCount), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val orderId = "BK-" + UUID.randomUUID().toString().take(8).uppercase(Locale.getDefault())
            val selectedDate = viewModel.dates.value.find { it.isSelected }
            val selectedTime = viewModel.times.value.find { it.isSelected }
            val ticketTotal = viewModel.totalPrice.value

            val booking = com.example.movie_booking_app.data.model.entity.Booking(
                orderId = orderId,
                movieId = intent.getStringExtra("movieId"),
                movieName = intent.getStringExtra("movieName") ?: "Movie",
                moviePoster = intent.getStringExtra("moviePoster"),
                movieGenre = intent.getStringExtra("movieGenre") ?: "",
                cinemaName = intent.getStringExtra("cinemaName") ?: "Cinema Center",
                cinemaAddress = intent.getStringExtra("cinemaAddress") ?: "Cinema Address",
                roomName = "Room 3",
                showDate = selectedDate?.id ?: "",
                showTime = selectedTime?.showTime ?: selectedTime?.startTime ?: "",
                duration = 120,
                language = "English",
                subtitle = "Vietnamese",
                seatIds = selectedSeatsList.map { it.id },
                seatNames = selectedSeatsList.map { it.label },
                ticketTypeDetails = emptyList(),
                combos = emptyList(),
                ticketTotal = ticketTotal,
                comboTotal = 0,
                bookingFee = 0,
                discount = 0,
                grandTotal = ticketTotal
            )

            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("BOOKING_SESSION", booking)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        binding.mainContent.visibility = View.GONE
                        binding.layoutSkeleton.root.visibility = View.VISIBLE
                        binding.layoutSkeleton.root.startShimmer()
                    } else {
                        binding.mainContent.visibility = View.VISIBLE
                        binding.layoutSkeleton.root.stopShimmer()
                        binding.layoutSkeleton.root.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seats.collect { list -> seatAdapter.submitList(list) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dates.collect { list -> dateAdapter.submitList(list) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.times.collect { list -> timeAdapter.submitList(list) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedSeats.collect { list ->
                    binding.btnContinue.isEnabled = list.isNotEmpty()
                    binding.btnContinue.alpha = if (list.isNotEmpty()) 1.0f else 0.5f
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalPrice.collect { price ->
                    binding.tvTotalAmount.text = formatCurrency(price)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    Toast.makeText(this@SelectSeatActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale.GERMANY)
        return "${formatter.format(amount)} VND"
    }
}
