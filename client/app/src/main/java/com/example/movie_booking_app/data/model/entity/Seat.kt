package com.example.movie_booking_app.data.model.entity

import com.example.movie_booking_app.R

enum class SeatType {
    NORMAL, VIP, EMPTY
}

/**
 * Updated to match develop branch naming while keeping our logic.
 */
enum class SeatStatus {
    AVAILABLE, RESERVED, SELECTED
}

/**
 * Enhanced Seat model: Compatible with develop branch but contains SRS logic and UI helpers.
 */
data class Seat(
    val id: String,         // e.g. "A1"
    val row: String,        // e.g. "A"
    val number: Int,        // e.g. 1 (Matches develop naming)
    val type: SeatType = SeatType.NORMAL,
    var status: SeatStatus = SeatStatus.AVAILABLE,
    val price: Int = 80000 
) {
    val label: String get() = "$row$number"

    /**
     * Logic tách rời: Quyết định màu sắc hiển thị dựa trên trạng thái ghế.
     * Đảm bảo pass Acceptance Criteria của task.
     */
    fun getBackgroundResource(): Int {
        return when (status) {
            SeatStatus.SELECTED -> R.drawable.bg_seat_selected
            SeatStatus.RESERVED -> R.drawable.bg_seat_sold
            SeatStatus.AVAILABLE -> {
                if (type == SeatType.VIP) R.drawable.bg_seat_vip else R.drawable.bg_seat_available
            }
        }
    }
}
