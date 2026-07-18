package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.LeaveRequestApi
import com.example.movie_booking_app.data.model.request.CreateLeaveRequest
import com.example.movie_booking_app.data.model.request.RejectLeaveRequest
import com.example.movie_booking_app.data.model.response.AcceptShiftResponse
import com.example.movie_booking_app.data.model.response.CreateLeaveResponse
import com.example.movie_booking_app.data.model.response.LeaveRequestResponse
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestResponse
import com.example.movie_booking_app.data.model.response.ReplacementShiftResponse
import com.example.movie_booking_app.data.model.response.SimpleSuccessResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRequestRepository @Inject constructor(private val api: LeaveRequestApi) {
    // --- Staff Flows ---
    fun createLeaveRequest(staffShiftId: String, reason: String): Flow<Result<CreateLeaveResponse?>> = flow {
        try {
            val response = api.createLeaveRequest(CreateLeaveRequest(staffShiftId, reason))
            if (response.isSuccessful) {
                emit(Result.success(response.body()))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to submit leave request"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    fun getReplacementShift() : Flow<List<ReplacementShiftResponse>?> = flow {
        try {
            val response = api.getReplacementShifts()
            if(response.isSuccessful) {
                emit(response.body()?.data)
            }else {
                emit(null)
            }

        }catch (e: Exception) {
            emit(null)
        }
    }

    fun getMyLeaveRequests(status: String?): Flow<Result<List<LeaveRequestResponse>>> = flow {
        try {
            val response = api.getMyLeaveRequests(status)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()?.data ?: emptyList()
                emit(Result.success(list))
            } else {
                emit(Result.failure(Exception(response.errorBody()?.string() ?: "Unknown Error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun acceptReplacementShift(shiftId: String) : Flow<AcceptShiftResponse?> = flow {
        try {
            val response = api.acceptReplacementShift(shiftId)
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }
        }catch (e: Exception) {
            emit(null)
        }
    }

    // --- Manager Flows ---
    fun getLeaveRequestForManager(status: String?) : Flow<List<ManagerLeaveRequestResponse>?> = flow {
        try {
            val response = api.getLeaveRequestsForManager(status)
            if(response.isSuccessful) {
                emit(response.body()?.data)
            }else {
                emit(null)
            }
        }catch (e: Exception) {
            emit(null)
        }
    }
    fun approveLeaveRequest(leaveRequestId: String) : Flow<SimpleSuccessResponse?> = flow {
        try {
            val response = api.approveLeaveRequest(leaveRequestId)
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }
        }catch (e: Exception){
            emit(null)
        }
    }

    fun rejectLeaveRequest(leaveRequestId: String, rejectReason: String) : Flow<SimpleSuccessResponse?> = flow {
        try {
            val response = api.rejectLeaveRequest(leaveRequestId,RejectLeaveRequest(rejectReason))
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }
        }catch (e: Exception) {
            emit(null)
        }
    }
    fun undoLeaveRequest(id: String): Flow<SimpleSuccessResponse?> = flow {
        try {
            val response = api.undoLeaveRequest(id)
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }
        }catch (e: Exception) {
            emit(null)
        }
    }
}