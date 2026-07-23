package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.databinding.ActivityPaymentBinding
import com.example.movie_booking_app.viewmodel.PaymentViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.example.movie_booking_app.utils.DateUtils
import kotlin.math.roundToInt

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModels()

    private var combo1Count = 0
    private var combo2Count = 0
    private var isExpiredDialogShown = false
    private var isSuccessDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val booking = intent.getSerializableExtra("BOOKING_SESSION") as? Booking
        if (booking == null) {
            Toast.makeText(this, "Invalid booking session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.initBooking(booking)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // Payment methods click listeners
        binding.cardMoMo.setOnClickListener { viewModel.selectPaymentMethod("MoMo") }
        binding.cardZaloPay.setOnClickListener { viewModel.selectPaymentMethod("ZaloPay") }
        binding.cardShopeePay.setOnClickListener { viewModel.selectPaymentMethod("ShopeePay") }
        binding.cardATM.setOnClickListener { viewModel.selectPaymentMethod("ATM") }
        binding.cardInternational.setOnClickListener { viewModel.selectPaymentMethod("International") }

        // Combo 1 Single Combo quantity selectors
        binding.btnCombo1Minus.setOnClickListener {
            if (combo1Count > 0) {
                combo1Count--
                binding.tvCombo1Qty.text = combo1Count.toString()
                viewModel.updateCombo1Quantity(combo1Count)
            }
        }
        binding.btnCombo1Plus.setOnClickListener {
            combo1Count++
            binding.tvCombo1Qty.text = combo1Count.toString()
            viewModel.updateCombo1Quantity(combo1Count)
        }

        // Combo 2 Double Combo quantity selectors
        binding.btnCombo2Minus.setOnClickListener {
            if (combo2Count > 0) {
                combo2Count--
                binding.tvCombo2Qty.text = combo2Count.toString()
                viewModel.updateCombo2Quantity(combo2Count)
            }
        }
        binding.btnCombo2Plus.setOnClickListener {
            combo2Count++
            binding.tvCombo2Qty.text = combo2Count.toString()
            viewModel.updateCombo2Quantity(combo2Count)
        }

        // Apply Promotion Coupon
        binding.btnApplyCoupon.setOnClickListener {
            val code = binding.etCouponCode.text.toString().trim()
            viewModel.applyCoupon(code)
        }

        // Listen for coupon code edits to reset applied coupon state
        binding.etCouponCode.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (viewModel.uiState.value.couponApplied) {
                    viewModel.removeCoupon()
                }
            }
        })

        // Reward Points Switch toggle listener
        binding.switchRewardPoints.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleRewardPoints(isChecked)
        }

        // Continue Button Checkout action
        binding.btnContinue.setOnClickListener {
            val selectedMethod = viewModel.uiState.value.selectedPaymentMethod
            if (selectedMethod == null) {
                Snackbar.make(binding.root, "Please select a payment method", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.confirmCheckout()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading overlay indicators
                    val isRunning = state.isLoading || state.isCheckingOut
                    binding.layoutLoading.visibility = if (isRunning) View.VISIBLE else View.GONE
                    binding.btnContinue.isEnabled = !state.isCheckingOut

                    // Reward Points availability and switch control
                    if (state.availablePoints <= 0) {
                        binding.switchRewardPoints.isEnabled = false
                        binding.tvAvailablePoints.text = "No reward points available"
                    } else {
                        binding.switchRewardPoints.isEnabled = true
                        binding.tvAvailablePoints.text = "Available: ${state.availablePoints} pts"
                    }

                    if (binding.switchRewardPoints.isChecked != state.useRewardPoints) {
                        binding.switchRewardPoints.isChecked = state.useRewardPoints
                    }

                    // Summary bindings
                    state.booking?.let { booking ->
                        binding.tvMovieName.text = booking.movieName ?: "Movie Title"
                        binding.tvGenre.text = booking.movieGenre ?: "Genres"
                        binding.tvCinemaName.text = booking.cinemaName ?: "Cinema Center"
                        binding.tvCinemaAddress.text = booking.cinemaAddress ?: "Address details"
                        binding.tvRoom.text = booking.roomName ?: "Room 1"
                        binding.tvShowDate.text = DateUtils.formatMovieDate(booking.showDate)
                        binding.tvShowTime.text = DateUtils.formatMovieTime(booking.showTime)

                        binding.tvOrderId.text = booking.orderId
                        binding.tvSeats.text = booking.seatNames.joinToString(", ")
                        binding.tvSeatCount.text = "${booking.seatIds.size} seats"
                        binding.tvSeatType.text = booking.seatTypes.joinToString(", ")

                        // Load Movie Poster
                        booking.moviePoster?.let { poster ->
                            val posterUrl = if (poster.startsWith("http")) poster 
                            else "${com.example.movie_booking_app.utils.Constants.BASE_URL.removeSuffix("/")}$poster"
                            
                            Glide.with(this@PaymentActivity)
                                .load(posterUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.stat_notify_error)
                                .into(binding.imgMoviePoster)
                        }
                    }

                    // Countdown text update
                    binding.tvCountdown.text = state.countdownText

                    // Countdown session expired trigger
                    if (state.isCountdownExpired && !isExpiredDialogShown) {
                        isExpiredDialogShown = true
                        showExpiredDialog()
                    }

                    // Observe checkoutSuccess to display Success dialog
                    if (state.checkoutSuccess && !isSuccessDialogShown) {
                        isSuccessDialogShown = true
                        showPremiumSuccessDialog("Booking completed successfully!") {
                            // Reload GET /profile from the server and navigate to Profile tab
                            val intent = Intent(this@PaymentActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("TARGET_SECTION", "Ticket")
                            }
                            startActivity(intent)
                            finish()
                        }
                    }

                    // Price Breakdown bindings
                    binding.tvTicketSubtotal.text = formatCurrency(state.ticketSubtotal)
                    binding.tvComboSubtotal.text = formatCurrency(state.comboSubtotal)
                    binding.tvDiscount.text = if (state.discountAmount > 0) "-${formatCurrency(state.discountAmount)}" else "0 VND"
                    binding.tvBookingFee.text = formatCurrency(state.bookingFee)
                    binding.tvGrandTotal.text = formatCurrency(state.grandTotal)
                    binding.tvFooterGrandTotal.text = formatCurrency(state.grandTotal)

                    // Reward points discount summary row
                    if (state.useRewardPoints && state.rewardDiscount > 0) {
                        binding.layoutRewardDiscount.visibility = View.VISIBLE
                        binding.tvRewardDiscount.text = "-${formatCurrency(state.rewardDiscount)} (${state.rewardDiscount / 10} pts)"
                    } else {
                        binding.layoutRewardDiscount.visibility = View.GONE
                    }

                    // Coupon validation messages & updates
                    if (state.couponApplied) {
                        binding.etCouponCode.setTextColor(Color.parseColor("#34C759"))
                    } else {
                        binding.etCouponCode.setTextColor(Color.parseColor("#FFFFFF"))
                    }

                    state.couponSuccessMessage?.let { msg ->
                        Toast.makeText(this@PaymentActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.resetErrors()
                    }

                    state.couponError?.let { err ->
                        binding.etCouponCode.error = err
                        Toast.makeText(this@PaymentActivity, err, Toast.LENGTH_SHORT).show()
                        viewModel.resetErrors()
                    }

                    // Selected payment highlighting
                    updatePaymentMethodsHighlight(state.selectedPaymentMethod)

                    // Generic exception alerts
                    state.error?.let { err ->
                        Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                        viewModel.resetErrors()
                    }
                }
            }
        }
    }

    private fun updatePaymentMethodsHighlight(selectedMethod: String?) {
        val activeColor = Color.parseColor("#F5C542")
        val inactiveColor = Color.TRANSPARENT

        binding.cardMoMo.strokeColor = if (selectedMethod == "MoMo") activeColor else inactiveColor
        binding.cardMoMo.strokeWidth = if (selectedMethod == "MoMo") dpToPx(2) else 0
        binding.rbMoMo.isChecked = (selectedMethod == "MoMo")

        binding.cardZaloPay.strokeColor = if (selectedMethod == "ZaloPay") activeColor else inactiveColor
        binding.cardZaloPay.strokeWidth = if (selectedMethod == "ZaloPay") dpToPx(2) else 0
        binding.rbZaloPay.isChecked = (selectedMethod == "ZaloPay")

        binding.cardShopeePay.strokeColor = if (selectedMethod == "ShopeePay") activeColor else inactiveColor
        binding.cardShopeePay.strokeWidth = if (selectedMethod == "ShopeePay") dpToPx(2) else 0
        binding.rbShopeePay.isChecked = (selectedMethod == "ShopeePay")

        binding.cardATM.strokeColor = if (selectedMethod == "ATM") activeColor else inactiveColor
        binding.cardATM.strokeWidth = if (selectedMethod == "ATM") dpToPx(2) else 0
        binding.rbATM.isChecked = (selectedMethod == "ATM")

        binding.cardInternational.strokeColor = if (selectedMethod == "International") activeColor else inactiveColor
        binding.cardInternational.strokeWidth = if (selectedMethod == "International") dpToPx(2) else 0
        binding.rbInternational.isChecked = (selectedMethod == "International")
    }

    private fun showPremiumSuccessDialog(message: String, onDismiss: () -> Unit) {
        val dialogBinding = com.example.movie_booking_app.databinding.DialogSuccessBinding.inflate(layoutInflater)

        dialogBinding.tvDialogMessage.text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Make background transparent to see rounded card corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnDialogOk.setOnClickListener {
            dialog.dismiss()
            onDismiss()
        }

        dialog.show()

        // Check icon pops with a small bounce animation
        dialogBinding.imgCheckCircle.scaleX = 0f
        dialogBinding.imgCheckCircle.scaleY = 0f
        dialogBinding.imgCheckCircle.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun showExpiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Booking Session Expired")
            .setMessage("Your 15-minute booking window has expired. Please select seats again.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                finish() // Returns back to SelectSeatActivity
            }
            .show()
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale.GERMANY)
        return "${formatter.format(amount)} VND"
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }
}
