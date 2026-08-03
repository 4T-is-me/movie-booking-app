package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.StaffApi
import com.example.movie_booking_app.data.model.request.CheckInTicketRequest
import com.example.movie_booking_app.data.model.response.CheckInTicketResponse
import com.example.movie_booking_app.data.model.response.ReviewListResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
    }.flowOn(Dispatchers.IO)

    fun checkInTicket(bookingCode: String): Flow<CheckInTicketResponse?> = flow {
        try {
            val response = api.checkInTicket(CheckInTicketRequest(bookingCode))
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorMsg = response.errorBody()?.string() ?: "An error occurred"
                val parsedError = try {
                    Gson().fromJson(errorMsg, CheckInTicketResponse::class.java)
                } catch (e: Exception) {
                    CheckInTicketResponse(false, errorMsg, null, null)
                }
                emit(parsedError)
            }
        } catch (e: Exception) {
            emit(CheckInTicketResponse(false, e.message ?: "Network error", null, null))
        }
    }.flowOn(Dispatchers.IO)

    fun getMyRatings(): Flow<ReviewListResponse?> = flow {
        try {
            val response = api.getMyRatings()
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
}
