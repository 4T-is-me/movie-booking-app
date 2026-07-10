package com.example.movie_booking_app.data.model.entity

import java.io.Serializable

data class Booking(
    val orderId: String,
    val movieId: String?,
    val movieName: String?,
    val moviePoster: String?,
    val movieGenre: String?,
    val cinemaName: String?,
    val cinemaAddress: String?,
    val roomName: String?,
    val showDate: String?,
    val showTime: String?,
    val duration: Int?,
    val language: String?,
    val subtitle: String?,
    val seatIds: List<String>,
    val seatNames: List<String>,
    val ticketTypeDetails: List<TicketTypeDetail>,
    val combos: List<ComboDetail>?,
    val ticketTotal: Int,
    val comboTotal: Int,
    val bookingFee: Int = 0,
    val discount: Int = 0,
    val grandTotal: Int,
    val promotionId: String? = null,
    val showtimeId: String? = null,
    val remainingSeconds: Int? = null
) : Serializable

data class TicketTypeDetail(
    val name: String,
    val count: Int,
    val price: Int
) : Serializable

data class ComboDetail(
    val comboId: String,
    val name: String,
    val quantity: Int,
    val price: Int
) : Serializable
