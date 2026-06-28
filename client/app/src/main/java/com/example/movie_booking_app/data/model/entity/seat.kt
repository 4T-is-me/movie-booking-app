package com.example.movie_booking_app.data.model.entity

enum class SeatType {
    NORMAL, VIP, EMPTY
}

enum class SeatStatus {
    AVAILABLE, SOLD, SELECTED
}

data class Seat(
    val id: String,
    val row: String,
    val col: Int,
    val type: SeatType = SeatType.NORMAL,
    var status: SeatStatus = SeatStatus.AVAILABLE
) {
    val label: String get() = "$row$col"
}
