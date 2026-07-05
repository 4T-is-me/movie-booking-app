package com.example.movie_booking_app.data.model.entity

import com.example.movie_booking_app.R
import com.google.gson.annotations.SerializedName

enum class SeatType {
    @SerializedName("Regular") NORMAL,
    @SerializedName("VIP") VIP,
    @SerializedName("Couple") COUPLE,
    EMPTY
}

enum class SeatStatus {
    AVAILABLE, RESERVED, SELECTED, HOLDING
}

data class Seat(
    @SerializedName("seatId")
    val id: String,
    @SerializedName("seatLabel")
    val label: String,
    val row: String,
    @SerializedName("col")
    val number: Int,
    @SerializedName("seatType")
    val type: SeatType = SeatType.NORMAL,
    @SerializedName("status")
    val rawStatus: String? = null,
    val price: Int = 0,
    
    @Transient
    var displayStatus: SeatStatus = SeatStatus.AVAILABLE
) {
    /**
     * Syncs the display status based on the raw status string from the backend.
     */
    fun syncStatus() {
        displayStatus = when (rawStatus) {
            "available" -> SeatStatus.AVAILABLE
            "booked" -> SeatStatus.RESERVED
            "holding" -> SeatStatus.HOLDING
            else -> SeatStatus.AVAILABLE
        }
    }

    /**
     * Returns the appropriate background resource based on seat status and type.
     */
    fun getBackgroundResource(): Int {
        return when (displayStatus) {
            SeatStatus.SELECTED -> R.drawable.bg_seat_selected
            SeatStatus.RESERVED, SeatStatus.HOLDING -> R.drawable.bg_seat_sold
            SeatStatus.AVAILABLE -> {
                if (type == SeatType.VIP) {
                    R.drawable.bg_seat_vip
                } else if (type == SeatType.COUPLE) {
                    R.drawable.bg_seat_couple
                } else {
                    R.drawable.bg_seat_available
                }
            }
        }
    }
}
