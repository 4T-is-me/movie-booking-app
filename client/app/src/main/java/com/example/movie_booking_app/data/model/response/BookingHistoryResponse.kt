package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName

data class BookingHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<BookingHistoryItem>?
)

data class BookingHistoryItem(
    @SerializedName("bookingId") val bookingId: String,
    @SerializedName("bookingCode") val bookingCode: String,
    @SerializedName("movie") val movie: String,
    @SerializedName("poster") val poster: String,
    @SerializedName("showDate") val showDate: String,
    @SerializedName("showtime") val showtime: String,
    @SerializedName("cinemaName") val cinemaName: String,
    @SerializedName("roomName") val roomName: String,
    @SerializedName("seats") val seats: List<String>?,
    @SerializedName("finalAmount") val finalAmount: Double,
    @SerializedName("paymentStatus") val paymentStatus: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("qrCodeData") val qrCodeData: String? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("isStaffRated") val isStaffRated: Boolean? = false,
    @SerializedName("checkedIn") val checkedIn: Boolean? = false
)
