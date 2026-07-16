package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        movieRepositoryRemoteImpl: MovieRepositoryRemoteImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindCinemaRoomRepository(
        impl: CinemaRoomRepositoryImpl
    ): CinemaRoomRepository

    @Binds
    @Singleton
    abstract fun bindCinemaRoomsRepository(
        impl: CinemaRoomsRepositoryImpl
    ): CinemaRoomsRepository

    @Binds
    @Singleton
    abstract fun bindCinemasRepository(
        impl: CinemasRepositoryImpl
    ): CinemasRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        impl: BookingRepositoryRemoteImpl // Must be RemoteImpl to call actual API
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(
        impl: MemberRepositoryImpl
    ): MemberRepository
}
