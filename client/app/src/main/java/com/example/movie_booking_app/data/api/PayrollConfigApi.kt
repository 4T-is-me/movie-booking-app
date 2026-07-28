package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.PayrollConfigResponse
import com.example.movie_booking_app.data.model.response.UpdatePayrollConfigRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface PayrollConfigApi {
    @GET("/api/admin/payroll-config")
    suspend fun getPayrollConfig(): Response<PayrollConfigResponse>

    @PUT("/api/admin/payroll-config")
    suspend fun updatePayrollConfig(@Body request: UpdatePayrollConfigRequest): Response<PayrollConfigResponse>
}
