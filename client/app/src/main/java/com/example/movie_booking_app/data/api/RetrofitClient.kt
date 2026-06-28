package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.baseUrl)
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
