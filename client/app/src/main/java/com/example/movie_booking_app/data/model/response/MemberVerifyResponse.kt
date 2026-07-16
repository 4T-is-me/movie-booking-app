package com.example.movie_booking_app.data.model.response

data class MemberVerifyResponse(
    val success: Boolean,
    val data: MemberVerifyData?
)

data class MemberVerifyData(
    val memberId: String,
    val memberName: String,
    val phoneNumber: String,
    val currentScore: Int
)
