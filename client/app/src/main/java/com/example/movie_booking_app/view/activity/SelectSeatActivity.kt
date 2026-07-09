package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupLegend()
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
        val layoutManager = GridLayoutManager(this, 10)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val seat = seatAdapter.currentList.getOrNull(position)
                return if (seat?.type == com.example.movie_booking_app.data.model.entity.SeatType.COUPLE) 2 else 1
            }
        }
        binding.rvSeats.layoutManager = layoutManager
        binding.rvSeats.adapter = seatAdapter
        binding.rvSeats.isNestedScrollingEnabled = false
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            if (viewModel.uiState.value == com.example.movie_booking_app.viewmodel.SeatingUiState.HELD) {
                viewModel.releaseCurrentHold()
            }
            finish()
        }
        
        binding.btnContinue.setOnClickListener {
            val selectedSeatsList = viewModel.selectedSeats.value
            if (selectedSeatsList.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_no_selection), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedTime = viewModel.times.value.find { it.isSelected }
            if (selectedTime == null) {
                Toast.makeText(this, getString(R.string.error_select_time), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.holdSeats(selectedTime.id, selectedSeatsList.map { it.id })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.holdSuccess.collect { success ->
                    if (success) {
                        viewModel.resetHoldSuccess()
                        val selectedSeatsList = viewModel.selectedSeats.value
                        val selectedDate = viewModel.dates.value.find { it.isSelected }
                        val selectedTime = viewModel.times.value.find { it.isSelected }
                        val ticketTotal = viewModel.totalPrice.value
                        val orderId = "BK-" + UUID.randomUUID().toString().take(8).uppercase(Locale.getDefault())

                        val booking = com.example.movie_booking_app.data.model.entity.Booking(
                            orderId = orderId,
                            movieId = intent.getStringExtra("movieId"),
                            movieName = intent.getStringExtra("movieName") ?: getString(R.string.unknown_movie),
                            moviePoster = intent.getStringExtra("moviePoster"),
                            movieGenre = intent.getStringExtra("movieGenre") ?: "",
                            cinemaName = intent.getStringExtra("cinemaName") ?: getString(R.string.cinema_center),
                            cinemaAddress = intent.getStringExtra("cinemaAddress") ?: "",
                            roomName = getString(R.string.default_room_name),
                            showDate = selectedDate?.id ?: "",
                            showTime = selectedTime?.showTime ?: selectedTime?.startTime ?: "",
                            duration = 120,
                            language = getString(R.string.default_language),
                            subtitle = getString(R.string.default_subtitle),
                            seatIds = selectedSeatsList.map { it.id },
                            seatNames = selectedSeatsList.map { it.label },
                            ticketTypeDetails = emptyList(),
                            combos = emptyList(),
                            ticketTotal = ticketTotal,
                            comboTotal = 0,
                            bookingFee = 0,
                            discount = 0,
                            grandTotal = ticketTotal,
                            showtimeId = selectedTime?.id
                        )

                        val intent = Intent(this@SelectSeatActivity, PaymentActivity::class.java).apply {
                            putExtra("BOOKING_SESSION", booking)
                        }
                        startActivity(intent)
                    }
                }
            }
        }

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
                viewModel.selectedSeatsTotalCount.collect { count ->
                    binding.btnContinue.isEnabled = count > 0
                    binding.btnContinue.alpha = if (count > 0) 1.0f else 0.5f
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerManager.remainingSeconds.collect { seconds ->
                    val isActive = viewModel.timerManager.isActive.value
                    binding.tvCountdown.visibility = if (isActive) View.VISIBLE else View.GONE
                    binding.tvCountdown.text = formatTime(seconds)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state == com.example.movie_booking_app.viewmodel.SeatingUiState.EXPIRED) {
                        showExpiredDialog()
                    }
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun showExpiredDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.title_expired))
        builder.setMessage(getString(R.string.msg_expired))
        builder.setPositiveButton(getString(R.string.btn_ok)) { dialog, _ ->
            dialog.dismiss()
            val selectedTime = viewModel.times.value.find { it.isSelected }
            selectedTime?.let { viewModel.selectTime(it.id) }
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale.GERMANY)
        return "${formatter.format(amount)} VND"
    }
}
