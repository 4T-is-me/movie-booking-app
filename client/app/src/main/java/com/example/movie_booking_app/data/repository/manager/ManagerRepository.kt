package com.example.movie_booking_app.data.repository.manager

import com.example.movie_booking_app.data.api.ManagerApi
import com.example.movie_booking_app.data.model.request.GenerateShiftRequest
import com.example.movie_booking_app.data.model.request.UpdateShiftRequest
import com.example.movie_booking_app.data.model.response.GenerateShiftApiResponse
import com.example.movie_booking_app.data.model.response.GenerateShiftResponse
import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import com.example.movie_booking_app.data.model.response.ManagerReportsResponse
import com.example.movie_booking_app.data.model.response.ManagerStaffListResponse
import com.example.movie_booking_app.data.model.response.ShiftDetailResponse
import com.example.movie_booking_app.data.model.response.ShiftResponse
import com.example.movie_booking_app.data.model.response.UpdateShiftResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

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

    fun getStaffShifts(startDate: String? = null, endDate: String? = null, status: String? = null): Flow<ShiftResponse?> = flow {
        try {
            val response = api.getStaffShift(startDate, endDate, status)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    fun getShiftDetail(shiftId: String): Flow<ShiftDetailResponse?> = flow {
        try {
            val response = api.getShiftDetail(shiftId)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getStaffForManager(): Flow<ManagerStaffListResponse?> = flow {
        try {
            val response = api.getManagerStaff()
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun generateShift(request: GenerateShiftRequest): Flow<GenerateShiftApiResponse?> = flow {
        try {
            val response = api.generateShift(request)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJson ?: "").getString("message")
                } catch (e: Exception) {
                    "Generate shifts failed"
                }
                emit(
                    GenerateShiftApiResponse(
                        success = false,
                        message = errorMessage,
                        data = GenerateShiftResponse(0, "", "", errorMessage)
                    )
                )
            }
        } catch (e: Exception) {
            emit(GenerateShiftApiResponse(success = false, message = e.message ?: "Network Error", data = GenerateShiftResponse(0, "", "", e.message ?: "Network Error")))
        }
    }
    fun updateShift(shiftId: String, request: UpdateShiftRequest): Flow<UpdateShiftResponse?> = flow {
        try {
            val response = api.updateShift(shiftId, request)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                val errorJsonString = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorJsonString ?: "").getString("message")
                } catch (e: Exception) {
                    "Shift update failed with status: ${response.code()}"
                }
                emit(UpdateShiftResponse(success = false, message = errorMessage, data = null))
            }
        } catch (e: Exception) {
            emit(UpdateShiftResponse(success = false, message = e.message ?: "Network Error", data = null))
        }
    }
}