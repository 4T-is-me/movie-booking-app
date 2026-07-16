package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class BookingDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: BookingDetailData?
)

data class BookingDetailData(
    @SerializedName("bookingId") val bookingId: String,
    @SerializedName("bookingCode") val bookingCode: String,
    @SerializedName("movie") val movie: BookingDetailMovie?,
    @SerializedName("cinema") val cinema: BookingDetailCinema?,
    @SerializedName("cinemaRoom") val cinemaRoom: BookingDetailRoom?,
    @SerializedName("showtime") val showtime: BookingDetailShowtime?,
    @SerializedName("seats") val seats: List<BookingDetailSeat>?,
    @SerializedName("combos") val combos: List<BookingDetailCombo>?,
    @SerializedName("payment") val payment: BookingDetailPayment?,
    @SerializedName("earnedPoints") val earnedPoints: Int,
    @SerializedName("membershipRank") val membershipRank: String?
)

data class BookingDetailMovie(
    @SerializedName("movieName") val movieName: String,
    @SerializedName("poster") val poster: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("movieGenre") val movieGenre: List<String>?
)

data class BookingDetailCinema(
    @SerializedName("cinemaName") val cinemaName: String
)

data class BookingDetailRoom(
    @SerializedName("roomName") val roomName: String
)

data class BookingDetailShowtime(
    @SerializedName("showDate") val showDate: String,
    @SerializedName("showtime") val showtime: String
)

data class BookingDetailSeat(
    @SerializedName("seatName") val seatName: String,
    @SerializedName("seatType") val seatType: String? = null,
    @SerializedName("price") val price: Int
)

data class BookingDetailCombo(
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Int,
    @SerializedName("subtotal") val subtotal: Int
)

data class BookingDetailPayment(
    @SerializedName("ticketPrice") val ticketPrice: Double,
    @SerializedName("comboPrice") val comboPrice: Double,
    @SerializedName("subTotalAmount") val subTotalAmount: Double,
    @SerializedName("disCountAmount") val disCountAmount: Double,
    @SerializedName("finalAmount") val finalAmount: Double,
    @SerializedName("paymentMethod") val paymentMethod: String
)
