package com.example.movie_booking_app.data.model.response

data class ComboItem(
    val name: String?,
    val quantity: Int?,
    val price: Double?
)

data class CheckInTicketData(
    val bookingId: String?,
    val bookingCode: String?,
    val checkedIn: Boolean?,
    val checkedInAt: String?,
    val checkedInBy: String?,
    val movieName: String?,
    val duration: Int?,
    val roomName: String?,
    val screenType: String?,
    val cinemaName: String?,
    val showDate: String?,
    val showTime: String?,
    val seatNames: List<String>?,
    val combos: List<ComboItem>?,
    val finalAmount: Double?
)

data class ErrorItem(
    val field: String?,
    val message: String?
)

data class CheckInTicketResponse(
    val success: Boolean,
    val message: String?,
    val errors: List<ErrorItem>?,
    val data: CheckInTicketData?
)
