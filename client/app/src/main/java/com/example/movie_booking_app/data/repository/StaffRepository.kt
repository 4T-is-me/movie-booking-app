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
}
