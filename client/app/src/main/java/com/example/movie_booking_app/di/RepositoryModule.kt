package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.MovieRepositoryRemoteImpl
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
}
