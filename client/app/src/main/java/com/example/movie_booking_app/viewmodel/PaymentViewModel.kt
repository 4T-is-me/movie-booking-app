package com.example.movie_booking_app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.data.model.request.ComboRequest
import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.PaymentRepository
import com.example.movie_booking_app.data.repository.ProfileRepository
import com.example.movie_booking_app.utils.BookingTimerManager
import android.content.Context
import com.example.movie_booking_app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PaymentRepository,
    private val profileRepository: ProfileRepository,
    private val movieRepository: MovieRepository,
    val timerManager: BookingTimerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private var discountLevelPercent = 0
    private var discountType: String? = null
    private var discountValue = 0.0

    fun initBooking(booking: Booking) {
        _uiState.value = BookingUiState(
            booking = booking,
            ticketSubtotal = booking.ticketTotal,
            comboSubtotal = booking.comboTotal,
            grandTotal = booking.grandTotal,
            bookingFee = booking.bookingFee
        )
        observeTimer()
        loadRewardPoints()
        loadAvailablePromotions()
        recalculateTotals()
    }

    private fun loadAvailablePromotions() {
        viewModelScope.launch {
            movieRepository.getPromos()
                .catch { _uiState.update { it.copy(availablePromotions = emptyList()) } }
                .collect { list ->
                    _uiState.update { it.copy(availablePromotions = list) }
                }
        }
    }

    private fun loadRewardPoints() {
        viewModelScope.launch {
            profileRepository.getUserProfile()
                .catch { _uiState.update { it.copy(availablePoints = 0) } }
                .collect { profile ->
                    _uiState.update { 
                        it.copy(availablePoints = maxOf(0, profile.rewardPoints ?: 0))
                    }
                    recalculateTotals()
                }
        }
    }

    fun toggleRewardPoints(usePoints: Boolean) {
        _uiState.update { it.copy(useRewardPoints = usePoints) }
        recalculateTotals()
    }

    private fun observeTimer() {
        viewModelScope.launch {
            timerManager.remainingSeconds.collect { seconds ->
                val minutes = seconds / 60
                val secs = seconds % 60
                _uiState.update { it.copy(countdownText = String.format("%02d:%02d", minutes, secs)) }
            }
        }
        viewModelScope.launch {
            timerManager.isExpired.collect { expired ->
                if (expired) {
                    _uiState.update { it.copy(isCountdownExpired = true) }
                }
            }
        }
    }

    fun selectPaymentMethod(method: String) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    private fun recalculateTotals() {
        val state = _uiState.value
        val ticketSub = state.ticketSubtotal
        val comboSub = state.comboSubtotal
        
        val subtotal = ticketSub + comboSub
        val rawPromotionDiscount = when (discountType) {
            "percentage" -> floor(subtotal * (discountValue / 100.0)).toInt()
            "fixed" -> discountValue.toInt()
            else -> floor(subtotal * (discountLevelPercent / 100.0)).toInt()
        }
        val promotionDiscount = minOf(maxOf(0, rawPromotionDiscount), subtotal)
        
        val afterPromotion = maxOf(0, subtotal - promotionDiscount)
        val availablePointsVal = maxOf(0, state.availablePoints) * 10
        val rewardDiscount = if (state.useRewardPoints) {
            minOf(availablePointsVal, afterPromotion)
        } else {
            0
        }
        
        val fee = state.bookingFee
        val grandTotal = maxOf(0, subtotal + fee - promotionDiscount - rewardDiscount)
        val pointsEarned = floor((grandTotal * 0.05) / 10.0).toInt()

        _uiState.update { it.copy(
            discountAmount = promotionDiscount,
            rewardDiscount = rewardDiscount,
            grandTotal = grandTotal,
            earnedPoints = pointsEarned
        ) }
    }

    fun removeCoupon() {
        discountLevelPercent = 0
        discountType = null
        discountValue = 0.0
        _uiState.update { it.copy(
            couponApplied = false,
            couponSuccessMessage = null,
            couponError = null,
            promotionId = null,
            discountAmount = 0
        ) }
        recalculateTotals()
    }

    fun applyCoupon(code: String) {
        if (code.trim().isEmpty()) {
            _uiState.update { it.copy(couponError = context.getString(R.string.error_valid_coupon)) }
            return
        }

        viewModelScope.launch {
            repository.applyPromotion(code)
                .onStart {
                    _uiState.update { it.copy(isLoading = true, couponError = null, couponSuccessMessage = null) }
                }
                .catch { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        couponError = exception.message ?: context.getString(R.string.error_invalid_coupon)
                    ) }
                }
                .collect { response ->
                    discountLevelPercent = response.data?.discountLevel ?: 0
                    discountType = response.data?.discountType
                    discountValue = response.data?.discountValue ?: 0.0
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        couponApplied = true,
                        couponSuccessMessage = response.message ?: context.getString(R.string.msg_coupon_applied),
                        promotionId = response.data?.id
                    ) }
                    recalculateTotals()
                }
        }
    }

    fun confirmCheckout() {
        val state = _uiState.value
        val booking = state.booking ?: return

        if (state.selectedPaymentMethod == null) {
            _uiState.update { it.copy(error = context.getString(R.string.error_select_payment)) }
            return
        }

        if (state.isCountdownExpired) {
            _uiState.update { it.copy(error = context.getString(R.string.error_booking_expired)) }
            return
        }

        val requestCombos = booking.combos?.map { 
            ComboRequest(it.comboId, it.name, it.quantity, it.price)
        } ?: emptyList()

        viewModelScope.launch {
            val request = ConfirmBookingRequest(
                showtimeId = booking.showtimeId ?: "",
                seatIds = booking.seatIds,
                useScore = state.useRewardPoints,
                useRewardPoints = state.useRewardPoints,
                paymentMethod = state.selectedPaymentMethod,
                promotionId = state.promotionId,
                combos = requestCombos
            )

            repository.confirmBooking(request)
                .onStart {
                    _uiState.update { it.copy(isCheckingOut = true, error = null) }
                }
                .catch { exception ->
                    val errorMsg = exception.message ?: context.getString(R.string.error_checkout_failed)
                    if (errorMsg.contains("Not enough reward points", ignoreCase = true)) {
                        _uiState.update { it.copy(
                            isCheckingOut = false,
                            useRewardPoints = false,
                            error = context.getString(R.string.error_insufficient_points)
                        ) }
                        recalculateTotals()
                    } else {
                        _uiState.update { it.copy(
                            isCheckingOut = false,
                            error = errorMsg
                        ) }
                    }
                }
                .collect {
                    try {
                        profileRepository.getUserProfile().collect {}
                    } catch (e: Exception) {
                        Log.e("PaymentViewModel", "Failed to refresh profile after checkout", e)
                    }
                    _uiState.update { it.copy(
                        isCheckingOut = false,
                        checkoutSuccess = true
                    ) }
                }
        }
    }

    fun resetErrors() {
        _uiState.update { it.copy(
            error = null,
            couponError = null,
            couponSuccessMessage = null
        ) }
    }
}
