package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.model.request.RateStaffRequest
import com.example.movie_booking_app.data.model.response.BookingHistoryResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.Response

@Singleton
class MyTicketRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    fun getMyTickets(): Flow<Response<BookingHistoryResponse>> = flow {
        emit(paymentApi.getMyTickets())
    }

    suspend fun rateStaff(bookingId: String, rating: Int, comment: String?): Response<ResponseBody> {
        val request = RateStaffRequest(
            rating = rating,
            comment = comment
        )
        return paymentApi.rateStaff(bookingId, request)
    }
}
