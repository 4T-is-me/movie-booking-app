package com.example.movie_booking_app.view.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.BookingDetailData
import com.example.movie_booking_app.databinding.ActivityBookingDetailBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.viewmodel.BookingDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@AndroidEntryPoint
class BookingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailBinding
    private val viewModel: BookingDetailViewModel by viewModels()
    private var bookingId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide status bar using WindowInsetsControllerCompat (Android 11+ compatible)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        if (bookingId.isEmpty()) {
            finish()
            return
        }

        setupUI()
        observeViewModel()

        viewModel.loadBookingDetail(bookingId)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadBookingDetail(bookingId)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        binding.shimmerContainer.visibility = View.VISIBLE
                        binding.shimmerContainer.startShimmer()
                        binding.layoutContent.visibility = View.GONE
                        binding.layoutErrorState.visibility = View.GONE
                    } else {
                        binding.shimmerContainer.stopShimmer()
                        binding.shimmerContainer.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookingDetail.collect { detail ->
                    if (detail != null && !viewModel.isLoading.value) {
                        bindDetails(detail)
                        binding.layoutContent.visibility = View.VISIBLE
                        binding.layoutErrorState.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        binding.layoutErrorState.visibility = View.VISIBLE
                        binding.tvErrorMsg.text = it
                        binding.layoutContent.visibility = View.GONE
                        viewModel.resetError()
                    }
                }
            }
        }
    }

    private fun bindDetails(detail: BookingDetailData) {
        // Movie details
        binding.tvMovieTitle.text = detail.movie?.movieName ?: "-"
        val duration = detail.movie?.duration ?: 0
        val genres = detail.movie?.movieGenre?.joinToString(", ") ?: ""
        binding.tvDurationGenre.text = "${duration} mins • ${genres.ifEmpty { "-" }}"

        // Poster
        val url = detail.movie?.poster ?: ""
        val finalUrl = when {
            url.isEmpty() -> ""
            url.startsWith("http") -> url
            else -> "${Constants.BASE_URL.removeSuffix("/")}$url"
        }
        Glide.with(this)
            .load(finalUrl.ifEmpty { null })
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .into(binding.ivPoster)

        // Membership
        val rank = detail.membershipRank ?: "bronze"
        val rankClean = rank.trim().lowercase()
        val formattedRank = rankClean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.tvMembershipRank.text = "$formattedRank Member"

        val badgeColor = when (rankClean) {
            "gold" -> Color.parseColor("#F5C542")
            "silver" -> Color.parseColor("#C0C0C0")
            else -> Color.parseColor("#CD7F32") // bronze
        }
        binding.tvMembershipRank.setTextColor(badgeColor)
        binding.cardMembershipRank.strokeColor = badgeColor

        // Showtime details
        binding.tvCinema.text = detail.cinema?.cinemaName ?: "-"
        binding.tvRoom.text = detail.cinemaRoom?.roomName ?: "-"
        binding.tvDate.text = detail.showtime?.showDate ?: "-"
        binding.tvTime.text = detail.showtime?.showtime ?: "-"

        // Seats
        val seatNames = detail.seats?.map { it.seatName } ?: emptyList()
        binding.tvSeats.text = seatNames.joinToString(", ").ifEmpty { "-" }

        // Seat Type
        val seatTypes = detail.seats?.mapNotNull { it.seatType }?.distinct() ?: emptyList()
        binding.tvSeatType.text = seatTypes.joinToString(", ").ifEmpty { "-" }

        // Currency Formatter
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)

        // Selected Combos List Setup
        binding.layoutComboItems.removeAllViews()
        val combos = detail.combos ?: emptyList()
        binding.cardCombos.visibility = View.VISIBLE

        if (combos.isEmpty()) {
            binding.tvNoCombo.visibility = View.VISIBLE
            binding.layoutComboItems.visibility = View.GONE
            binding.dividerCombo.visibility = View.GONE
            binding.layoutComboTotal.visibility = View.GONE
            binding.layoutComboPrice.visibility = View.GONE
        } else {
            binding.tvNoCombo.visibility = View.GONE
            binding.layoutComboItems.visibility = View.VISIBLE
            binding.dividerCombo.visibility = View.VISIBLE
            binding.layoutComboTotal.visibility = View.VISIBLE
            binding.layoutComboPrice.visibility = View.VISIBLE

            var totalComboPrice = 0
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            combos.forEach { combo ->
                totalComboPrice += combo.subtotal

                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, dp4, 0, dp4)
                    }
                }

                val nameTv = TextView(this).apply {
                    text = "• ${combo.name} × ${combo.quantity}"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val priceTv = TextView(this).apply {
                    text = "  ${formatter.format(combo.subtotal)} VND"
                    setTextColor(Color.parseColor("#88FFFFFF"))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                itemLayout.addView(nameTv)
                itemLayout.addView(priceTv)
                binding.layoutComboItems.addView(itemLayout)
            }
            binding.tvComboTotal.text = "${formatter.format(totalComboPrice)} VND"
        }

        // Payment info
        val pay = detail.payment
        if (pay != null) {
            binding.tvTicketPrice.text = "${formatter.format(pay.ticketPrice)} VND"
            binding.tvComboPrice.text = "${formatter.format(pay.comboPrice)} VND"

            // Promotion discount
            val discount = pay.disCountAmount
            if (discount > 0) {
                binding.layoutPromotion.visibility = View.VISIBLE
                binding.tvPromotionDiscount.text = "-${formatter.format(discount)} VND"
            } else {
                binding.layoutPromotion.visibility = View.GONE
            }

            // Points Earned
            binding.tvEarnedPoints.text = "+${detail.earnedPoints} Points"

            // Payment Method
            binding.tvPaymentMethod.text = "Paid via ${pay.paymentMethod}"

            // Total Paid
            binding.tvTotalPaid.text = "${formatter.format(pay.finalAmount)} VND"
        }
    }
}
