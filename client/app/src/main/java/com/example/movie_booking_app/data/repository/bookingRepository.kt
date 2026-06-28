package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.entity.ShowTime
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    // Thêm date để sơ đồ ghế khác nhau theo từng ngày
    fun getSeats(date: String, time: String): Flow<List<Seat>>
    fun getShowDates(movieId: String): Flow<List<ShowDate>>
    fun getShowTimes(movieId: String, date: String): Flow<List<ShowTime>>
}
