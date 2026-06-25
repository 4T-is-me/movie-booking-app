package com.example.movie_booking_app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 is the special alias to your host loopback interface (localhost) for Android emulator
    private const val BASE_URL = "http://10.88.55.126:5000/"

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
}
