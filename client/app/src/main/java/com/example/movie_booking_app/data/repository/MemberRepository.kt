package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.response.MemberVerifyResponse
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun verifyMember(identity: String): Flow<MemberVerifyResponse>
}
