package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.CinemaRoomApi
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.request.AddRoomRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CinemaRoomsRepositoryImpl @Inject constructor(
    private val api: CinemaRoomApi
) : CinemaRoomsRepository {
    override fun getRooms(cinemaId: String?): Flow<List<CinemaRoom>> = flow {
        val response = api.getRooms(cinemaId)
        if (response.isSuccessful) {
            emit(response.body()?.data ?: emptyList())
        } else {
            throw Exception(response.message() ?: "Failed to fetch cinema rooms")
        }
    }

    override suspend fun createRoom(request: AddRoomRequest): Boolean {
        val response = api.createRoom(request)
        return response.isSuccessful
    }

    override suspend fun getRoomDetails(roomId: String): CinemaRoom? {
        return try {
            val response = api.getRoomDetails(roomId)
            if (response.isSuccessful) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateRoom(roomId: String, request: AddRoomRequest): Boolean {
        return try {
            val response = api.updateRoom(roomId, request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateRoomStatus(roomId: String, status: String): Boolean {
        return try {
            val response = api.updateRoomStatus(roomId, com.example.movie_booking_app.data.model.request.UpdateStatusRequest(status))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getRoomSeats(roomId: String): List<com.example.movie_booking_app.data.model.entity.SeatPreview> {
        return try {
            val response = api.getRoomSeats(roomId)
            if (response.isSuccessful) {
                val groupedRows = response.body()?.data ?: emptyList()
                groupedRows.flatMap { row ->
                    row.seats?.map { seat ->
                        com.example.movie_booking_app.data.model.entity.SeatPreview(
                            seatName = seat.seatName ?: "",
                            seatType = (seat.seatType ?: "standard").lowercase()
                        )
                    } ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
