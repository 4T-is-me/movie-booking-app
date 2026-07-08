package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.model.response.BookingHistoryResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    fun getBookingHistory(): Flow<Response<BookingHistoryResponse>> = flow {
        emit(paymentApi.getBookingHistory())
    }

    fun getBookingDetail(bookingId: String): Flow<Response<com.example.movie_booking_app.data.model.response.BookingDetailResponse>> = flow {
        emit(paymentApi.getBookingDetail(bookingId))
    }
}
