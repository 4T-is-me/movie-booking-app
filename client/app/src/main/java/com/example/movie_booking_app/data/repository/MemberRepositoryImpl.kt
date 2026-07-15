package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.MemberApi
import com.example.movie_booking_app.data.model.response.MemberVerifyResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberApi: MemberApi
) : MemberRepository {

    override fun verifyMember(identity: String): Flow<MemberVerifyResponse> = flow {
        val response = memberApi.verifyMember(identity)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            if (response.code() == 404) {
                emit(MemberVerifyResponse(success = false, data = null))
            } else {
                throw Exception("Error verifying member: ${response.message()}")
            }
        }
    }
}
