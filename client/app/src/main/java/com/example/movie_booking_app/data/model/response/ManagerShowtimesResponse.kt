package com.example.movie_booking_app.data.model.response

data class ManagerShowtimesResponse(
    val success: Boolean,
    val data: List<ManagerShowtimeItem>
)

data class ManagerShowtimeItem(
    val _id: String,
    val movieId: ManagerShowtimeMovie?,
    val roomId: ManagerShowtimeRoom?,
    val showDate: String,
    val startTime: String,
    val endTime: String,
    val status: String
)

data class ManagerShowtimeMovie(
    val _id: String,
    val movieName: String,
    val duration: Int
)

data class ManagerShowtimeRoom(
    val _id: String,
    val roomName: String
)
