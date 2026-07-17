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
}
