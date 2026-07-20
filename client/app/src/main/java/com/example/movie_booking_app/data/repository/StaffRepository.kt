package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.StaffApi
import com.example.movie_booking_app.data.api.LeaveRequestParams
import com.example.movie_booking_app.data.model.response.ShiftResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepository @Inject constructor(private val api: StaffApi) {

    fun getStaffShifts(startDate: String? = null, endDate: String? = null): Flow<ShiftResponse?> = flow {
        try {
            val response = api.getStaffShifts(startDate, endDate)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun checkInTicket(bookingCode: String): Flow<com.example.movie_booking_app.data.model.response.CheckInTicketResponse?> = flow {
        try {
            val response = api.checkInTicket(com.example.movie_booking_app.data.model.request.CheckInTicketRequest(bookingCode))
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorMsg = response.errorBody()?.string() ?: "An error occurred"
                val parsedError = try {
                    com.google.gson.Gson().fromJson(errorMsg, com.example.movie_booking_app.data.model.response.CheckInTicketResponse::class.java)
                } catch (e: Exception) {
                    com.example.movie_booking_app.data.model.response.CheckInTicketResponse(false, errorMsg, null, null)
                }
                emit(parsedError)
            }
        } catch (e: Exception) {
            emit(com.example.movie_booking_app.data.model.response.CheckInTicketResponse(false, e.message ?: "Network error", null, null))
        }
    }
}
