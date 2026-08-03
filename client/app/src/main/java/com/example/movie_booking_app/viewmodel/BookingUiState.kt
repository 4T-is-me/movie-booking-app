package com.example.movie_booking_app.viewmodel

import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.data.model.entity.Promo

data class BookingUiState(
    val booking: Booking? = null,
    val countdownText: String = "10:00",
    val isCountdownExpired: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCheckingOut: Boolean = false,
    val checkoutSuccess: Boolean = false,
    val couponApplied: Boolean = false,
    val couponError: String? = null,
    val couponSuccessMessage: String? = null,
    val ticketSubtotal: Int = 0,
    val comboSubtotal: Int = 0,
    val discountAmount: Int = 0,
    val bookingFee: Int = 0,
    val grandTotal: Int = 0,
    val promotionId: String? = null,
    val selectedPaymentMethod: String? = null,
    val availablePoints: Int = 0,
    val useRewardPoints: Boolean = false,
    val rewardDiscount: Int = 0,
    val earnedPoints: Int = 0,
    val availablePromotions: List<Promo> = emptyList()
)
