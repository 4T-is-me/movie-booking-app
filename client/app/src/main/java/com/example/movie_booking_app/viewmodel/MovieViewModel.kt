package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

    private val repository: MovieRepository = RepositoryProvider.provideMovieRepository()
    private val _searchQuery = MutableStateFlow("")
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItem>>(initialHomeState())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    init {
        loadHomeData()
    }

    private fun initialHomeState(): List<HomeItem> = listOf(
        HomeItem.HeaderSection("User"),
        HomeItem.NowPlayingSection(null, isLoading = true),
        HomeItem.ComingSoonSection(null, isLoading = true),
        HomeItem.PromoSection(null, isLoading = true),
        HomeItem.ServiceSection(null, isLoading = true),
        HomeItem.MovieNewsSection(null, isLoading = true)
    )

    private fun loadHomeData() {
        viewModelScope.launch {
            combine(
                repository.getMovies().onStart { emit(emptyList()) },
                repository.getPromos().onStart { emit(emptyList()) },
                repository.getServices().onStart { emit(emptyList()) },
                repository.getNews().onStart { emit(emptyList()) },
                _searchQuery
            ) { movies, promos, services, news, query ->
                
                val filteredMovies = if (query.isEmpty()) movies else {
                    movies.filter { it.movieName?.contains(query, ignoreCase = true) == true }
                }

                if (filteredMovies.isEmpty() && query.isNotEmpty()) {
                    listOf(HomeItem.HeaderSection("User"), HomeItem.EmptyState)
                } else {
                    val list = mutableListOf<HomeItem>()
                    list.add(HomeItem.HeaderSection("User"))
                    
                    val nowPlaying = filteredMovies.filter { it.status == "showing" }
                    list.add(HomeItem.NowPlayingSection(nowPlaying.ifEmpty { null }, nowPlaying.isEmpty() && query.isEmpty()))
                    
                    val comingSoon = filteredMovies.filter { it.status == "coming_soon" }
                    list.add(HomeItem.ComingSoonSection(comingSoon.ifEmpty { null }, comingSoon.isEmpty() && query.isEmpty()))
                    
                    if (query.isEmpty()) {
                        list.add(HomeItem.PromoSection(promos.ifEmpty { null }, promos.isEmpty()))
                        list.add(HomeItem.ServiceSection(services.ifEmpty { null }, services.isEmpty()))
                        list.add(HomeItem.MovieNewsSection(news.ifEmpty { null }, news.isEmpty()))
                    }
                    list
                }
            }.collect {
                _homeItems.value = it
                _isLoading.value = false
            }
        }
    }

    fun searchMovies(query: String) {
        _searchQuery.value = query
    }
}
