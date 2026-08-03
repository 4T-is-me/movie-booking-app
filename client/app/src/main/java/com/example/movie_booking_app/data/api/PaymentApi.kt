package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.model.request.RateStaffRequest
import com.example.movie_booking_app.data.model.response.BookingDetailResponse
import com.example.movie_booking_app.data.model.response.BookingHistoryResponse
import com.example.movie_booking_app.data.model.response.PromotionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
        @Path("id") id: String
    ): Response<BookingDetailResponse>

    @POST("api/booking/{id}/rate-staff")
    suspend fun rateStaff(
        @Path("id") bookingId: String,
        @Body request: RateStaffRequest
    ): Response<ResponseBody>
}
