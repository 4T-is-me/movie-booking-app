package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.GeneratePayrollRequest
import com.example.movie_booking_app.data.model.response.GenericMessageResponse
import com.example.movie_booking_app.data.model.response.PayrollDetailResponse
import com.example.movie_booking_app.data.model.response.PayrollListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PayrollApi {
    @POST("/api/payrolls/generate")
    suspend fun generatePayroll(@Body request: GeneratePayrollRequest): Response<GenericMessageResponse>

    @GET("/api/payrolls")
    suspend fun getPayrolls(
        @Query("month") month: Int?,
        @Query("year") year: Int?,
        @Query("cinemaId") cinemaId: String?,
        @Query("status") status: String?
    ): Response<PayrollListResponse>

    @GET("/api/payrolls/manager")
    suspend fun getManagerPayrolls(): Response<PayrollListResponse>

    @GET("/api/payrolls/staff")
    suspend fun getStaffPayrolls(): Response<PayrollListResponse>
    @GET("/api/payrolls/{id}")
    suspend fun getPayrollById( @Path("id") id: String): Response<PayrollDetailResponse>

    @PATCH("/api/payrolls/{id}/pay")
    suspend fun markAsPaid( @Path("id") id: String): Response<GenericMessageResponse>
}

