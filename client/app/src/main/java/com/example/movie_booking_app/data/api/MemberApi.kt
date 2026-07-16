package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.response.MemberVerifyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MemberApi {
    @GET("api/members/{identity}")
    suspend fun verifyMember(@Path("identity") identity: String): Response<MemberVerifyResponse>
}
