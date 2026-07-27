package com.example.movie_booking_app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class ResolveMaintenanceRequest(
    val action: String // "accept" or "decline"
)

data class ResolveMaintenanceResponse(
    val success: Boolean,
    val message: String? = null
)

interface MaintenanceApi {
    @POST("api/maintenance/bookings/{id}/resolve-maintenance")
    suspend fun resolveMaintenance(
        @Path("id") bookingId: String,
        @Body request: ResolveMaintenanceRequest
    ): Response<ResolveMaintenanceResponse>
}
