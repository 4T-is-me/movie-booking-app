package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.model.response.BookingHistoryResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyTicketRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    fun getMyTickets(): Flow<Response<BookingHistoryResponse>> = flow {
        emit(paymentApi.getMyTickets())
    }
}
