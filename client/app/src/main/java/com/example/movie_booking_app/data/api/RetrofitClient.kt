package com.example.movie_booking_app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 is the special alias to your host loopback interface (localhost) for Android emulator
    // Note for LDPlayer: If 10.0.2.2 doesn't work, change this to your PC's current IPv4 address (run ipconfig to check).
    private const val BASE_URL = "http://172.20.10.6:5000/"


    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        instance.create(AuthApi::class.java)
    }

    val movieApi: MovieApi by lazy {
        instance.create(MovieApi::class.java)
    }

    val roomApi: CinemaRoomApi by lazy {
        instance.create(CinemaRoomApi::class.java)
    }
}
