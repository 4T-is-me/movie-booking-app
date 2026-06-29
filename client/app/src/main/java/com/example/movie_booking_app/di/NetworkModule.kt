package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.MovieApi
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
        return com.example.movie_booking_app.data.api.RetrofitClient.movieApi
    }

    @Provides
    @Singleton
    fun provideProfileApi(): com.example.movie_booking_app.data.api.ProfileApi {
        return com.example.movie_booking_app.data.api.RetrofitClient.profileApi
    }
}
