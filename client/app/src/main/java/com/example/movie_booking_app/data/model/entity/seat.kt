package com.example.movie_booking_app.data.model.entity

/**
 * Enumeration representing the selection state of a cinema seat.
 */
enum class SeatStatus {
    AVAILABLE,
    RESERVED,
    SELECTED
}

/**
 * Data class representing a single seat in the cinema.
 */
data class Seat(
    val id: String,         // e.g. "A1"
    val row: String,        // e.g. "A"
    val number: Int,        // e.g. 1
    var status: SeatStatus = SeatStatus.AVAILABLE,
    val price: Int = 105000 // 105,000 VND per seat
)
