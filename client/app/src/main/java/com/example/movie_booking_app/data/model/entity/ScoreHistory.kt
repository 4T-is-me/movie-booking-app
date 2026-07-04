package com.example.movie_booking_app.data.model.entity

import Member

data class ScoreHistory(
    val _id: String,
    val memberId: Member,
    val bookingId: Booking,
    val usedPoints: Int,
    val earnedPoints: Int,
    val remainingPoints: Int,
    val reason: String,
    val createdAt: String
)