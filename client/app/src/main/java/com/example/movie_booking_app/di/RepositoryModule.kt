package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.MovieRepositoryRemoteImpl
import com.example.movie_booking_app.data.repository.CinemaRoomRepository
import com.example.movie_booking_app.data.repository.CinemaRoomRepositoryImpl
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.BookingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn

import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMovieRepository(
        movieRepositoryRemoteImpl: MovieRepositoryRemoteImpl
    ): MovieRepository

    @Binds
    abstract fun bindCinemaRoomRepository(
        impl: CinemaRoomRepositoryImpl
    ): CinemaRoomRepository

    @Binds
    abstract fun bindBookingRepository(
        impl: BookingRepositoryImpl
    ): BookingRepository
}
