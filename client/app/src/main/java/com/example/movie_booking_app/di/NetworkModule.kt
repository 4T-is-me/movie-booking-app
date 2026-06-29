package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.CinemaRoomApi
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.api.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMovieApi(): MovieApi {
        return RetrofitClient.movieApi
    }

    @Provides
    @Singleton
    fun provideCinemaRoomApi(): CinemaRoomApi {
        return RetrofitClient.roomApi
    }
}
