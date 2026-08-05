package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.data.model.entity.SeatType
import com.example.movie_booking_app.databinding.ActivitySelectSeatBinding
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.viewmodel.SeatingUiState
import com.example.movie_booking_app.viewmodel.SelectSeatViewModel
import com.example.movie_booking_app.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Activity for Cinema Seat Selection screen.
 */
@AndroidEntryPoint
class SelectSeatActivity : BaseActivity() {

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
        setupSystemBackHandling()
        observeViewModel()
    }

    private fun setupSystemBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.uiState.value == SeatingUiState.HELD) {
                    viewModel.releaseCurrentHold()
                }
                finish()
            }
        })
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
                return if (seat?.type == SeatType.COUPLE) 2 else 1
            }
        }
        binding.rvSeats.layoutManager = layoutManager
        binding.rvSeats.adapter = seatAdapter
        // Optimization: Removing isNestedScrollingEnabled = false to allow view recycling
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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

                        val seatConstants = Constants.SeatConstants
                        val booking = Booking(
                            orderId = orderId,
                            movieId = intent.getStringExtra("movieId"),
                            movieName = intent.getStringExtra("movieName") ?: getString(R.string.unknown_movie),
                            moviePoster = intent.getStringExtra("moviePoster"),
                            movieGenre = intent.getStringExtra("movieGenre") ?: "",
                            cinemaName = viewModel.cinemaName.value.takeIf { !it.isNullOrEmpty() }
                                ?: selectedTime?.cinemaId?.cinemaName?.takeIf { !it.isNullOrEmpty() }
                                ?: intent.getStringExtra("cinemaName")
                                ?: getString(R.string.cinema_center),
                            cinemaAddress = intent.getStringExtra("cinemaAddress") ?: "",
                            roomName = viewModel.cinemaRoomName.value.takeIf { !it.isNullOrEmpty() } ?: getString(R.string.default_room_name),
                            showDate = selectedDate?.id ?: "",
                            showTime = selectedTime?.showTime ?: selectedTime?.startTime ?: "",
                            duration = Constants.BookingConfig.DEFAULT_MOVIE_DURATION,
                            language = getString(R.string.default_language),
                            subtitle = getString(R.string.default_subtitle),
                            seatIds = selectedSeatsList.map { it.id },
                            seatNames = selectedSeatsList.map { it.label },
                            seatTypes = selectedSeatsList.map { seat ->
                                when (seat.type) {
                                    SeatType.NORMAL -> seatConstants.TYPE_REGULAR
                                    SeatType.VIP -> seatConstants.TYPE_VIP
                                    SeatType.COUPLE -> seatConstants.TYPE_COUPLE
                                    else -> seatConstants.TYPE_REGULAR
                                }
                            }.distinct(),
                            ticketTypeDetails = emptyList(),
                            combos = emptyList(),
                            ticketTotal = ticketTotal,
                            comboTotal = 0,
                            bookingFee = 0,
                            discount = 0,
                            grandTotal = ticketTotal,
                            showtimeId = selectedTime?.id
                        )

                        val intent = Intent(this@SelectSeatActivity, SelectFoodActivity::class.java).apply {
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
                    binding.tvTotalAmount.text = price.toVnd()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    val displayMsg = when (message) {
                        Constants.ErrorCode.NO_SHOWTIMES -> getString(R.string.error_no_showtimes)
                        Constants.ErrorCode.NO_SEATS -> getString(R.string.error_no_seats)
                        Constants.ErrorCode.SAME_TYPE -> getString(R.string.error_same_type)
                        Constants.ErrorCode.HOLD_CONFLICT -> getString(R.string.error_hold_conflict)
                        Constants.ErrorCode.HOLD_FAILED -> getString(R.string.error_hold_failed)
                        else -> message
                    }
                    Toast.makeText(this@SelectSeatActivity, displayMsg, Toast.LENGTH_LONG).show()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerManager.remainingSeconds.collect { seconds ->
                    val isActive = viewModel.timerManager.isActive.value
                    binding.tvCountdown.visibility = if (isActive) View.VISIBLE else View.GONE
                    binding.tvCountdown.text = seconds.toTimerFormat()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state == SeatingUiState.EXPIRED) {
                        showExpiredDialog()
                    }
                }
            }
        }
    }

    private fun showExpiredDialog() {
        val builder = AlertDialog.Builder(this)
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
}

/**
 * Local extensions for SelectSeatActivity
 */
private fun Int.toVnd(): String {
    val formatter = NumberFormat.getInstance(Locale.GERMANY)
    return "${formatter.format(this)} VND"
}

private fun Int.toTimerFormat(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
