package com.example.movie_booking_app.data.repository.manager

import com.example.movie_booking_app.data.api.ManagerApi
import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import com.example.movie_booking_app.data.model.response.ManagerReportsResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagerRepository @Inject constructor(private val api: ManagerApi) {
    fun getDashboard(): Flow<ManagerDashboardResponse?> = flow {
        try {
            val response = api.getDashboardData()
            if (response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getManagerReport(type: String): Flow<ManagerReportsResponse?> = flow {
        try {
            val response = api.getReportData(type)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getStaffShifts(startDate: String? = null, endDate: String? = null): Flow<ShiftResponse?> = flow {
        try {
            val response = api.getStaffShift(startDate, endDate)
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