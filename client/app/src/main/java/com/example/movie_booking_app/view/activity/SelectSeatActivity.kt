package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivitySelectSeatBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.viewmodel.SelectSeatViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity for Cinema Seat Selection screen.
 */
class SelectSeatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectSeatBinding
    private val viewModel: SelectSeatViewModel by lazy {
        ViewModelProvider(this)[SelectSeatViewModel::class.java]
    }

    private lateinit var seatAdapter: SeatAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    /**
     * Set up recyclerview layouts and binding adapters.
     */
    private fun setupRecyclerViews() {
        // Seat layout: 10 columns
        seatAdapter = SeatAdapter { seat ->
            viewModel.toggleSeatSelection(seat.id)
        }
        binding.rvSeats.layoutManager = GridLayoutManager(this, 10)
        binding.rvSeats.adapter = seatAdapter
        binding.rvSeats.isNestedScrollingEnabled = false

        // Date layout: horizontal
        dateAdapter = DateAdapter { date ->
            viewModel.selectDate(date.id)
        }
        binding.rvDates.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false,
        )
        binding.rvDates.adapter = dateAdapter

        // Time layout: horizontal
        timeAdapter = TimeAdapter { showtime  ->
            viewModel.selectTime(showtime.id)
        }
        binding.rvTimes.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false,
        )
        binding.rvTimes.adapter = timeAdapter
    }

    /**
     * Attach button listeners.
     */
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnBuyTicket.setOnClickListener {
            val price = viewModel.totalPrice.value
            if (price == 0) {
                Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Booking confirmed: ${formatCurrency(price)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Observe ViewModel state flows and sync to UI.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seats.collect { list ->
                    seatAdapter.submitList(list)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dates.collect { list ->
                    dateAdapter.submitList(list)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.times.collect { list ->
                    timeAdapter.submitList(list)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalPrice.collect { price ->
                    binding.tvTotalPrice.text = formatCurrency(price)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(this@SelectSeatActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Formats currency amounts with thousand separators (dots) matching mockup presentation style.
     */
    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale.GERMANY) // Germany formats digits using dot thousands separator (e.g. 210.000)
        return "${formatter.format(amount)} VND"
    }
}
