package com.example.movie_booking_app.data.model.response

import com.example.movie_booking_app.data.model.entity.ScoreHistory

data class ScoreHistoryBaseResponse(
    val success: Boolean,
    val data: List<ScoreHistory>
)