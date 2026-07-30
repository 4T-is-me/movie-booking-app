package com.example.movie_booking_app.view.fragment.staff

import androidx.core.content.ContextCompat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentStaffSalesBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.view.adapter.StaffMovieAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.view.adapter.StaffPromotionAdapter
import com.example.movie_booking_app.viewmodel.staff.StaffSalesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class StaffSalesFragment : Fragment() {

    private var _binding: FragmentStaffSalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffSalesViewModel by viewModels()

    private lateinit var movieAdapter: StaffMovieAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter
    private lateinit var seatAdapter: SeatAdapter
    private lateinit var promotionAdapter: StaffPromotionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupLegend()
        setupListeners()
        observeViewModel()

        viewModel.loadMovies()
    }

    private fun setupAdapters() {
        // Movies Grid Adapter
        movieAdapter = StaffMovieAdapter { movie ->
            viewModel.selectMovie(movie)
        }
        binding.rvMoviesGrid.adapter = movieAdapter

        // Date Adapter
        dateAdapter = DateAdapter { date ->
            viewModel.selectDate(date.id)
        }
        binding.rvDates.adapter = dateAdapter

        // Showtime Adapter
        timeAdapter = TimeAdapter { showtime ->
            viewModel.loadSeats(showtime.id)
        }
        binding.rvShowtimes.adapter = timeAdapter

        // Seat Adapter
        seatAdapter = SeatAdapter { seat ->
            viewModel.toggleSeatSelection(seat.id)
        }
        binding.rvSeatMap.adapter = seatAdapter

        // Promotion Adapter
        promotionAdapter = StaffPromotionAdapter { promo ->
            viewModel.applyPromoCode(promo.discountCode)
            binding.edtPromoCode.setText(promo.discountCode)
        }
        binding.rvAvailablePromotions.adapter = promotionAdapter
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

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (viewModel.currentStep.value > 1) {
                viewModel.prevStep()
            } else {
                viewModel.selectMovie(null)
            }
        }

        binding.btnNext.setOnClickListener {
            val step = viewModel.currentStep.value
            if (step == 1) {
                // Populate order details for step 2
                val movie = viewModel.selectedMovie.value
                val selectedShowtime = viewModel.showtimes.value.find { it.isSelected }
                val showtimeStr = selectedShowtime?.showTime ?: "N/A"
                val dateStr = selectedShowtime?.showDate ?: "N/A"
                val seatLabels = viewModel.selectedSeats.value.joinToString(", ") { it.label }
                val price = viewModel.totalPrice.value
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(price)

                binding.tvOrderMovieTitle.text = movie?.movieName ?: "N/A"
                binding.tvOrderDetails.text = "Showtime: $showtimeStr • Date: $dateStr • Seats: $seatLabels"
                binding.tvOrderPrice.text = "Subtotal: $formattedPrice"

                viewModel.nextStep()
            } else if (step == 2) {
                // Populate Step 3 order details
                val movie = viewModel.selectedMovie.value
                val selectedShowtime = viewModel.showtimes.value.find { it.isSelected }
                val showtimeStr = selectedShowtime?.showTime ?: "N/A"
                val dateStr = selectedShowtime?.showDate ?: "N/A"
                val seatLabels = viewModel.selectedSeats.value.joinToString(", ") { it.label }
                val price = viewModel.totalPrice.value
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(price)

                binding.tvPromoOrderMovieTitle.text = movie?.movieName ?: "N/A"
                binding.tvPromoOrderDetails.text = "Showtime: $showtimeStr • Date: $dateStr • Seats: $seatLabels"
                binding.tvPromoOrderPrice.text = "Subtotal: $formattedPrice"

                viewModel.loadAvailablePromotions()
                viewModel.nextStep()
            } else if (step == 3) {
                // Populate Step 4 (Confirmation details)
                val movie = viewModel.selectedMovie.value
                val selectedShowtime = viewModel.showtimes.value.find { it.isSelected }
                val showtimeStr = selectedShowtime?.showTime ?: "N/A"
                val dateStr = selectedShowtime?.showDate ?: "N/A"
                val seatLabels = viewModel.selectedSeats.value.joinToString(", ") { it.label }
                val member = viewModel.verifiedMember.value

                binding.tvFinalMovie.text = movie?.movieName ?: "N/A"
                binding.tvFinalShowtime.text = "Showtime: $showtimeStr • Date: $dateStr"
                binding.tvFinalSeats.text = "Seats: $seatLabels"
                binding.tvFinalMember.text = if (member != null) {
                    "Member: ${member.memberName} (${member.phoneNumber})"
                } else {
                    "Customer: Walk-in"
                }

                viewModel.nextStep()
            }
        }

        binding.btnVerifyMember.setOnClickListener {
            val identity = binding.edtMemberIdentity.text.toString()
            viewModel.verifyMember(identity)
        }

        binding.btnApplyPromo.setOnClickListener {
            val code = binding.edtPromoCode.text.toString()
            viewModel.applyPromoCode(code)
        }

        binding.swUseScore.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleUseScore(isChecked)
            binding.layoutScoreInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.edtScoreToRedeem.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString()
                val score = if (input.isEmpty()) 0 else input.toIntOrNull() ?: 0
                viewModel.setScoreToRedeem(score)
            }
        })

        binding.btnConfirmBooking.setOnClickListener {
            val paymentMethod = if (binding.rbCash.isChecked) "cash" else "credit_card"
            viewModel.confirmBooking(paymentMethod)
        }
    }

    private fun observeViewModel() {
        // Observe Loading State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observe Movies List
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movies.collectLatest { movies ->
                movieAdapter.submitList(movies)
            }
        }

        // Observe Dates Flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dates.collectLatest { dates ->
                dateAdapter.submitList(dates)
            }
        }

        // Observe Selected Movie to switch views
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedMovie.collectLatest { selectedMovie ->
                if (selectedMovie != null) {
                    binding.rvMoviesGrid.visibility = View.GONE
                    val step = viewModel.currentStep.value
                    binding.layoutSeatSelection.visibility = if (step == 1) View.VISIBLE else View.GONE
                    binding.layoutMemberVerification.visibility = if (step == 2) View.VISIBLE else View.GONE
                    binding.layoutPromotionAndScore.visibility = if (step == 3) View.VISIBLE else View.GONE
                    binding.layoutConfirmOrder.visibility = if (step == 4) View.VISIBLE else View.GONE
                    binding.btnBack.visibility = View.VISIBLE

                    binding.tvSelectedMovieTitle.text = selectedMovie.movieName
                    val genreStr = selectedMovie.movieGenre?.joinToString(", ") ?: ""
                    val durationStr = if (selectedMovie.duration != null) requireContext().getString(com.example.movie_booking_app.R.string.duration_mins, selectedMovie.duration.toString()) else "N/A"
                    binding.tvSelectedMovieInfo.text = "$genreStr | $durationStr"
                    binding.tvSelectedMovieRating.text = String.format("%.1f", selectedMovie.rating ?: 0.0)

                    Glide.with(requireContext())
                        .load(selectedMovie.poster)
                        .placeholder(R.color.card_dark)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.imgSelectedMoviePoster)
                } else {
                    binding.rvMoviesGrid.visibility = View.VISIBLE
                    binding.layoutSeatSelection.visibility = View.GONE
                    binding.layoutMemberVerification.visibility = View.GONE
                    binding.layoutPromotionAndScore.visibility = View.GONE
                    binding.layoutConfirmOrder.visibility = View.GONE
                    binding.btnBack.visibility = View.GONE
                }
            }
        }

        // Observe Showtimes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showtimes.collectLatest { showtimes ->
                timeAdapter.submitList(showtimes)
                movieAdapter.setSelectedMovie(viewModel.selectedMovie.value?.id)
            }
        }

        // Observe Seats
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seats.collectLatest { seatList ->
                if (seatList.isNotEmpty()) {
                    val maxCol = seatList.maxOfOrNull { it.number } ?: 10
                    binding.rvSeatMap.layoutManager = GridLayoutManager(requireContext(), maxCol)
                    seatAdapter.submitList(seatList)

                    binding.svSeatMapContainer.visibility = View.VISIBLE
                    binding.tvScreenLabel.visibility = View.VISIBLE
                    binding.viewScreenLine.visibility = View.VISIBLE
                    binding.layoutLegends.visibility = View.VISIBLE
                    binding.layoutSummary.visibility = View.VISIBLE
                } else {
                    binding.svSeatMapContainer.visibility = View.GONE
                    binding.tvScreenLabel.visibility = View.GONE
                    binding.viewScreenLine.visibility = View.GONE
                    binding.layoutLegends.visibility = View.GONE
                    binding.layoutSummary.visibility = View.GONE
                }
            }
        }

        // Observe Selected Seats and Price to enable/disable Next button
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSeats.collectLatest { selected ->
                if (selected.isNotEmpty()) {
                    val seatLabels = selected.joinToString(", ") { it.label }
                    binding.tvSelectedSeats.text = "Selected seats: $seatLabels"
                    if (viewModel.currentStep.value == 1) {
                        binding.btnNext.isEnabled = true
                        binding.btnNext.alpha = 1.0f
                    }
                } else {
                    binding.tvSelectedSeats.text = "Selected seats: None"
                    if (viewModel.currentStep.value == 1) {
                        binding.btnNext.isEnabled = false
                        binding.btnNext.alpha = 0.5f
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPrice.collectLatest { price ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(price)
                binding.tvTotalPrice.text = "Subtotal: $formattedPrice"
            }
        }

        // Observe Current Step
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStep.collectLatest { step ->
                binding.tvStepText.text = "$step of 4"

                val isMovieSelected = viewModel.selectedMovie.value != null
                binding.btnBack.visibility = if (isMovieSelected || step > 1) View.VISIBLE else View.GONE
                binding.btnNext.visibility = if (step < 4) View.VISIBLE else View.GONE

                updateStepIndicators(step)

                when (step) {
                    1 -> {
                        binding.tvHeaderTitle.text = "SELECT MOVIE & SHOWTIME"
                        val isMovieSelected = viewModel.selectedMovie.value != null
                        binding.layoutSeatSelection.visibility = if (isMovieSelected) View.VISIBLE else View.GONE
                        binding.layoutMemberVerification.visibility = View.GONE
                        binding.layoutPromotionAndScore.visibility = View.GONE
                        binding.layoutConfirmOrder.visibility = View.GONE
                        val seatsSelected = viewModel.selectedSeats.value.isNotEmpty()
                        binding.btnNext.isEnabled = seatsSelected
                        binding.btnNext.alpha = if (seatsSelected) 1.0f else 0.5f
                    }
                    2 -> {
                        binding.tvHeaderTitle.text = "MEMBER VERIFICATION"
                        binding.layoutSeatSelection.visibility = View.GONE
                        binding.layoutMemberVerification.visibility = View.VISIBLE
                        binding.layoutPromotionAndScore.visibility = View.GONE
                        binding.layoutConfirmOrder.visibility = View.GONE
                        binding.btnNext.isEnabled = true
                        binding.btnNext.alpha = 1.0f
                    }
                    3 -> {
                        binding.tvHeaderTitle.text = "PROMOTIONS & REWARDS"
                        binding.layoutSeatSelection.visibility = View.GONE
                        binding.layoutMemberVerification.visibility = View.GONE
                        binding.layoutPromotionAndScore.visibility = View.VISIBLE
                        binding.layoutConfirmOrder.visibility = View.GONE
                        val hasScoreError = viewModel.scoreError.value != null
                        binding.btnNext.isEnabled = !hasScoreError
                        binding.btnNext.alpha = if (hasScoreError) 0.5f else 1.0f
                    }
                    4 -> {
                        binding.tvHeaderTitle.text = "CONFIRM & CHECKOUT"
                        binding.layoutSeatSelection.visibility = View.GONE
                        binding.layoutMemberVerification.visibility = View.GONE
                        binding.layoutPromotionAndScore.visibility = View.GONE
                        binding.layoutConfirmOrder.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observe Member Verification Results to update Points redemption UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verifiedMember.collectLatest { member ->
                if (member != null) {
                    binding.cardMemberResult.visibility = View.VISIBLE
                    binding.tvMemberName.text = "Full name: ${member.memberName}"
                    binding.tvMemberPhone.text = "Phone number: ${member.phoneNumber}"
                    binding.tvMemberScore.text = "Accumulated points: ${member.currentScore} pts"
                    binding.cardWalkInAlert.visibility = View.GONE

                    // Step 3 points redemption
                    binding.cardPointsRedemption.visibility = View.VISIBLE
                    binding.tvPromoMemberPoints.text = "Available points: ${member.currentScore} pts"
                } else {
                    binding.cardMemberResult.visibility = View.GONE
                    binding.cardWalkInAlert.visibility = View.VISIBLE

                    // Step 3 points redemption
                    binding.cardPointsRedemption.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.memberVerificationError.collectLatest { error ->
                if (error != null) {
                    binding.tvMemberError.text = error
                    binding.tvMemberError.visibility = View.VISIBLE
                } else {
                    binding.tvMemberError.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isVerifyingMember.collectLatest { isVerifying ->
                binding.btnVerifyMember.isEnabled = !isVerifying
                binding.btnVerifyMember.text = if (isVerifying) "VERIFYING..." else "VERIFY"
            }
        }

        // Observe Promotions List
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availablePromotions.collectLatest { list ->
                promotionAdapter.submitList(list)
            }
        }

        // Observe Applied Promotion & Price Breakdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appliedPromotion.collectLatest { promo ->
                if (promo != null) {
                    binding.tvFinalPromoApplied.visibility = View.VISIBLE
                    binding.tvFinalPromoApplied.text = "Promotion: ${promo.title}"
                } else {
                    binding.tvFinalPromoApplied.visibility = View.GONE
                }
            }
        }

        // Observe Promotion Errors & Messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promoError.collectLatest { error ->
                if (error != null) {
                    binding.tvPromoError.text = error
                    binding.tvPromoError.visibility = View.VISIBLE
                } else {
                    binding.tvPromoError.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promoSuccessMessage.collectLatest { successMsg ->
                if (successMsg != null) {
                    binding.tvPromoMessage.text = successMsg
                    binding.tvPromoMessage.visibility = View.VISIBLE
                } else {
                    binding.tvPromoMessage.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isApplyingPromo.collectLatest { isApplying ->
                binding.btnApplyPromo.isEnabled = !isApplying
                binding.btnApplyPromo.text = if (isApplying) "APPLYING..." else "APPLY"
            }
        }

        // Observe Calculations for Step 3 Summary
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPrice.collectLatest { price ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvSummaryOriginalPrice.text = formatter.format(price)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promoDiscount.collectLatest { discount ->
                if (discount > 0) {
                    binding.layoutSummaryPromoDiscount.visibility = View.VISIBLE
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    binding.tvSummaryPromoDiscount.text = "-${formatter.format(discount)}"
                } else {
                    binding.layoutSummaryPromoDiscount.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pointDiscount.collectLatest { discount ->
                if (discount > 0) {
                    binding.layoutSummaryPointDiscount.visibility = View.VISIBLE
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    binding.tvSummaryPointDiscount.text = "-${formatter.format(discount)}"
                    binding.tvFinalPointsUsed.visibility = View.VISIBLE
                    binding.tvFinalPointsUsed.text = "Use points: Yes (Discount ${formatter.format(discount)})"
                } else {
                    binding.layoutSummaryPointDiscount.visibility = View.GONE
                    binding.tvFinalPointsUsed.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.grandTotal.collectLatest { total ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvSummaryGrandTotal.text = formatter.format(total)
                binding.tvFinalGrandTotal.text = formatter.format(total)
            }
        }

        // Observe Checkout States
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCheckingOut.collectLatest { isChecking ->
                binding.btnConfirmBooking.isEnabled = !isChecking
                binding.btnConfirmBooking.text = if (isChecking) "CHECKING OUT..." else "CONFIRM CHECKOUT"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.checkoutError.collectLatest { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.checkoutSuccess.collectLatest { success ->
                if (success) {
                    val code = viewModel.checkoutCode.value ?: "N/A"
                    showSuccessDialog("Ticket sold successfully!\nBooking ID: $code\n\nPrinting receipt and ticket...")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scoreToRedeem.collectLatest { score ->
                val currentText = binding.edtScoreToRedeem.text.toString()
                val parsedVal = currentText.toIntOrNull() ?: 0
                if (parsedVal != score) {
                    binding.edtScoreToRedeem.setText(score.toString())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scoreError.collectLatest { error ->
                if (error != null) {
                    binding.tvScoreError.text = error
                    binding.tvScoreError.visibility = View.VISIBLE
                    if (viewModel.currentStep.value == 3) {
                        binding.btnNext.isEnabled = false
                        binding.btnNext.alpha = 0.5f
                    }
                    binding.btnConfirmBooking.isEnabled = false
                } else {
                    binding.tvScoreError.visibility = View.GONE
                    if (viewModel.currentStep.value == 3) {
                        binding.btnNext.isEnabled = true
                        binding.btnNext.alpha = 1.0f
                    }
                    binding.btnConfirmBooking.isEnabled = !viewModel.isCheckingOut.value
                }
            }
        }
    }

    private fun showSuccessDialog(message: String) {
        val dialogBinding = com.example.movie_booking_app.databinding.DialogSuccessBinding.inflate(layoutInflater)
        dialogBinding.tvDialogMessage.text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnDialogOk.setOnClickListener {
            dialog.dismiss()
            viewModel.selectMovie(null)
        }

        dialog.show()
    }

    private fun updateStepIndicators(step: Int) {
        val activeDrawable = R.drawable.bg_selected_item
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.auto_color_232A45)

        binding.viewStep1.setBackgroundResource(if (step >= 1) activeDrawable else 0)
        if (step < 1) binding.viewStep1.setBackgroundColor(inactiveColor)

        binding.viewStep2.setBackgroundResource(if (step >= 2) activeDrawable else 0)
        if (step < 2) binding.viewStep2.setBackgroundColor(inactiveColor)

        binding.viewStep3.setBackgroundResource(if (step >= 3) activeDrawable else 0)
        if (step < 3) binding.viewStep3.setBackgroundColor(inactiveColor)

        binding.viewStep4.setBackgroundResource(if (step >= 4) activeDrawable else 0)
        if (step < 4) binding.viewStep4.setBackgroundColor(inactiveColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
