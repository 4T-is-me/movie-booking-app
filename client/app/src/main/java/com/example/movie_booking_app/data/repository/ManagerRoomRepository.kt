package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.ManagerRoomApi
import com.example.movie_booking_app.data.model.response.ManagerRoomListResponse
import com.example.movie_booking_app.data.model.response.ManagerSeatListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

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

    fun updateRoom(roomId: String, request: com.example.movie_booking_app.data.api.UpdateRoomRequest): Flow<Boolean> = flow {
        try {
            val response = api.updateRoom(roomId, request)
            emit(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)
}
