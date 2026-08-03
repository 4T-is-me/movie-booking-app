package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.BulkUpdateSeatStatusRequest
import com.example.movie_booking_app.data.api.ManagerRoomApi
import com.example.movie_booking_app.data.api.UpdateRoomRequest
import com.example.movie_booking_app.data.api.UpdateSeatStatusRequest
import com.example.movie_booking_app.data.model.response.ManagerRoomListResponse
import com.example.movie_booking_app.data.model.response.ManagerSeatListResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class ManagerRoomRepository @Inject constructor(
    private val api: ManagerRoomApi
) {
    fun getRooms(): Flow<ManagerRoomListResponse?> = flow {
        try {
            val response = api.getRooms()
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun getRoomSeats(roomId: String): Flow<ManagerSeatListResponse?> = flow {
        try {
            val response = api.getRoomSeats(roomId)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun updateRoom(roomId: String, request: UpdateRoomRequest): Flow<Boolean> = flow {
        try {
            val response = api.updateRoom(roomId, request)
            emit(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    fun updateSeatStatus(roomId: String, seatId: String, status: String): Flow<Boolean> = flow {
        try {
            val response = api.updateSeatStatus(roomId, seatId, UpdateSeatStatusRequest(status))
            emit(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    fun bulkUpdateSeatStatus(roomId: String, seatIds: List<String>, status: String): Flow<Boolean> = flow {
        try {
            val response = api.bulkUpdateSeatStatus(roomId, BulkUpdateSeatStatusRequest(seatIds, status))
            emit(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)
}
