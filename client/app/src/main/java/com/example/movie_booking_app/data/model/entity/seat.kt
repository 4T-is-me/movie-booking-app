package com.example.movie_booking_app.data.model.entity

import com.example.movie_booking_app.R

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

    /**
     * Logic tách rời: Quyết định màu sắc hiển thị dựa trên trạng thái ghế.
     */
    fun getBackgroundResource(): Int {
        return when (status) {
            SeatStatus.SELECTED -> R.drawable.bg_seat_selected
            SeatStatus.SOLD -> R.drawable.bg_seat_sold
            SeatStatus.AVAILABLE -> {
                if (type == SeatType.VIP) R.drawable.bg_seat_vip else R.drawable.bg_seat_available
            }
        }
    }
}
