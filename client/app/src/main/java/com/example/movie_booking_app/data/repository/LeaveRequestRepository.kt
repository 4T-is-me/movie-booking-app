package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.LeaveRequestApi
import com.example.movie_booking_app.data.model.request.CreateLeaveRequest
import com.example.movie_booking_app.data.model.request.RejectLeaveRequest
import com.example.movie_booking_app.data.model.response.AcceptShiftResponse
import com.example.movie_booking_app.data.model.response.CreateLeaveResponse
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
    fun createLeaveRequest(staffShiftId: String, reason: String): Flow<CreateLeaveResponse?> = flow {
        try {
            val response = api.createLeaveRequest(CreateLeaveRequest(staffShiftId, reason))
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }

        }catch (e: Exception) {
            emit(null)
        }
    }
    fun getReplacementShift() : Flow<List<ReplacementShiftResponse>?> = flow {
        try {
            val response = api.getReplacementShifts()
            if(response.isSuccessful) {
                emit(response.body())
            }else {
                emit(null)
            }

        }catch (e: Exception) {
            emit(null)
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