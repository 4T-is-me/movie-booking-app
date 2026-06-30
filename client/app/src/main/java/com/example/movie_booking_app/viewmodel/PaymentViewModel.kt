package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.data.model.entity.ComboDetail
import com.example.movie_booking_app.data.model.request.ComboRequest
import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var discountLevelPercent = 0

    private var combo1Quantity = 0
    private var combo2Quantity = 0

    fun initBooking(booking: Booking) {
        val total = booking.ticketTotal + booking.bookingFee
        _uiState.value = BookingUiState(
            booking = booking,
            ticketSubtotal = booking.ticketTotal,
            grandTotal = total
        )
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remainingSeconds = 900 // 15 mins (15 * 60 = 900)
            while (remainingSeconds >= 0) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                val timeStr = String.format("%02d:%02d", minutes, seconds)
                
                _uiState.value = _uiState.value.copy(
                    countdownText = timeStr,
                    isCountdownExpired = remainingSeconds == 0
                )
                if (remainingSeconds == 0) break
                delay(1000)
                remainingSeconds--
            }
        }
    }

    fun selectPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
    }

    fun updateCombo1Quantity(quantity: Int) {
        if (quantity < 0) return
        combo1Quantity = quantity
        recalculateTotals()
    }

    fun updateCombo2Quantity(quantity: Int) {
        if (quantity < 0) return
        combo2Quantity = quantity
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val ticketSub = _uiState.value.ticketSubtotal
        val comboSub = (combo1Quantity * 80000) + (combo2Quantity * 120000)
        
        val subtotal = ticketSub + comboSub
        val discount = Math.floor(subtotal * (discountLevelPercent / 100.0)).toInt()
        val fee = _uiState.value.bookingFee
        val grandTotal = subtotal + fee - discount

        _uiState.value = _uiState.value.copy(
            comboSubtotal = comboSub,
            discountAmount = discount,
            grandTotal = grandTotal
        )
    }

    fun applyCoupon(code: String) {
        if (code.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(couponError = "Please enter a valid coupon code")
            return
        }

        viewModelScope.launch {
            repository.applyPromotion(code)
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, couponError = null, couponSuccessMessage = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        couponError = exception.message ?: "Invalid or expired coupon"
                    )
                }
                .collect { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        couponApplied = true,
                        couponSuccessMessage = response.message ?: "Coupon applied!",
                        promotionId = response.data?.id
                    )
                    discountLevelPercent = response.data?.discountLevel ?: 0
                    recalculateTotals()
                }
        }
    }

    fun confirmCheckout() {
        val state = _uiState.value
        val booking = state.booking ?: return

        if (state.selectedPaymentMethod == null) {
            _uiState.value = _uiState.value.copy(error = "Please select a payment method")
            return
        }

        if (state.isCountdownExpired) {
            _uiState.value = _uiState.value.copy(error = "Booking session expired")
            return
        }

        val requestCombos = mutableListOf<ComboRequest>()
        if (combo1Quantity > 0) {
            requestCombos.add(ComboRequest("combo1", "Single Combo", combo1Quantity, 80000))
        }
        if (combo2Quantity > 0) {
            requestCombos.add(ComboRequest("combo2", "Double Combo", combo2Quantity, 120000))
        }

        viewModelScope.launch {
            val request = ConfirmBookingRequest(
                showtimeId = booking.showTime ?: "1",
                seatIds = booking.seatIds,
                useScore = false,
                paymentMethod = state.selectedPaymentMethod,
                promotionId = state.promotionId,
                combos = requestCombos
            )

            repository.confirmBooking(request)
                .onStart {
                    _uiState.value = _uiState.value.copy(isCheckingOut = true, error = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingOut = false,
                        error = exception.message ?: "Checkout failed"
                    )
                }
                .collect {
                    _uiState.value = _uiState.value.copy(
                        isCheckingOut = false,
                        checkoutSuccess = true
                    )
                }
        }
    }

    fun resetErrors() {
        _uiState.value = _uiState.value.copy(
            error = null,
            couponError = null,
            couponSuccessMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
