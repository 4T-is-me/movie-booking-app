package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.model.response.PromotionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

import com.example.movie_booking_app.data.model.response.BookingHistoryResponse
import retrofit2.http.GET

interface PaymentApi {
    @POST("api/promotions/apply")
    suspend fun applyPromotion(
        @Body request: Map<String, String>
    ): Response<PromotionResponse>

    @POST("api/booking/checkout")
    suspend fun confirmBooking(
        @Body request: ConfirmBookingRequest
    ): Response<ResponseBody>

    @GET("api/booking/my-tickets")
    suspend fun getMyTickets(): Response<BookingHistoryResponse>

    @GET("api/booking/history")
    suspend fun getBookingHistory(): Response<BookingHistoryResponse>

    @GET("api/booking/{id}")
    suspend fun getBookingDetail(
        @retrofit2.http.Path("id") id: String
    ): Response<com.example.movie_booking_app.data.model.response.BookingDetailResponse>
}
