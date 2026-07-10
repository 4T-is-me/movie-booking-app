package com.example.movie_booking_app.data.model.request

data class ConfirmBookingRequest(
    val showtimeId: String,
    val seatIds: List<String>,
    val useScore: Boolean,
    val useRewardPoints: Boolean,
    val paymentMethod: String,
    val promotionId: String?,
    val combos: List<ComboRequest>
)

data class ComboRequest(
    val comboId: String,
    val name: String,
    val quantity: Int,
    val price: Int
)
