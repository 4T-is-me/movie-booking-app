package com.example.movie_booking_app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.MovieResponse
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList

@HiltViewModel
class MovieViewModel @Inject constructor(
    application: Application,
    private val repository: MovieRepository,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItem>>(initialHomeState())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private val _movieDetail = MutableStateFlow<MovieResponse?>(null)
    val movieDetail: StateFlow<MovieResponse?> = _movieDetail.asStateFlow()
    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private val _movieShowtimes = MutableStateFlow<List<Showtime>>(emptyList())
    val movieShowtimes: StateFlow<List<Showtime>> = _movieShowtimes

    private val _showtimeError = MutableStateFlow<String?>(null)
    val showtimeError: StateFlow<String?> = _showtimeError
    init {
        loadHomeData()
    }

    private fun initialHomeState(): List<HomeItem> = listOf(
        HomeItem.HeaderSection(getCachedUsername(), isLoggedIn = checkIsLoggedIn()),
        HomeItem.NowPlayingSection(null, isLoading = true),
        HomeItem.ComingSoonSection(null, isLoading = true),
        HomeItem.PromoSection(null, isLoading = true),
        HomeItem.ServiceSection(null, isLoading = true),
        HomeItem.MovieNewsSection(null, isLoading = true)
    )

    private fun getCachedUsername(): String {
        val sharedPref = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("username", "User") ?: "User"
    }

    private fun checkIsLoggedIn(): Boolean {
        val sharedPref = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        return !token.isNullOrEmpty()
    }

    fun loadHomeData() {
        val cachedName = getCachedUsername()
        val currentlyLoggedIn = checkIsLoggedIn()

        // User profile flow, defaults to cached values on start/error
        val userFlow = profileRepository.getUserProfile()
            .map { (it.username ?: cachedName) to true }
            .catch { emit(cachedName to currentlyLoggedIn) }
            .onStart { emit(cachedName to currentlyLoggedIn) }

        viewModelScope.launch {
            combine(
                repository.getMovies().onStart { emit(emptyList()) },
                repository.getPromos().onStart { emit(emptyList()) },
                repository.getServices().onStart { emit(emptyList()) },
                repository.getNews().onStart { emit(emptyList()) },
                combine(userFlow, _searchQuery) { userState, query -> userState to query }
            ) { movies, promos, services, news, userAndQuery ->
                val userName = userAndQuery.first.first
                val isLoggedIn = userAndQuery.first.second
                val query = userAndQuery.second
                
                val filteredMovies = if (query.isEmpty()) movies else {
                    movies.filter { it.movieName?.contains(query, ignoreCase = true) == true }
                }

                if (filteredMovies.isEmpty() && query.isNotEmpty()) {
                    listOf(HomeItem.HeaderSection(userName, isLoggedIn), HomeItem.EmptyState)
                } else {
                    val list = mutableListOf<HomeItem>()
                    list.add(HomeItem.HeaderSection(userName, isLoggedIn))
                    
                    val nowPlaying = filteredMovies.filter { 
                        val s = it.status?.lowercase() ?: ""
                        s.contains("showing") || s == "active" || s == "now_showing"
                    }
                    list.add(HomeItem.NowPlayingSection(nowPlaying.ifEmpty { null }, nowPlaying.isEmpty() && query.isEmpty()))
                    
                    val comingSoon = filteredMovies.filter { 
                        val s = it.status?.lowercase() ?: ""
                        s.contains("coming") || s.contains("soon") || s == "upcoming" || s == "inactive"
                    }
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

    fun fetchMovieDetail(id: String) {
        viewModelScope.launch {
            _movieDetail.value = null
            _detailError.value = null

            repository.getMovieDetailsById(id)
                .catch { e ->
                    _detailError.value = "Error: ${e.localizedMessage}"
                }
                .collect { movieData ->
                    _movieDetail.value = MovieResponse(data = movieData)
                }
        }
    }

    fun fetchMovieShowtimes(movieId: String) {
        viewModelScope.launch {
            _movieShowtimes.value = emptyList()
            _showtimeError.value = null

            repository.getMovieShowtimes(movieId)
                .catch { e ->
                    _showtimeError.value = "Error: ${e.localizedMessage}"
                }
                .collect { showtimes ->
                    _movieShowtimes.value = showtimes
                }
        }
    }
    private val _movieActionSuccess = MutableSharedFlow<Boolean>()
    val movieActionSuccess = _movieActionSuccess.asSharedFlow()

    fun deleteMovie(id: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteMovie(id)
                _movieActionSuccess.emit(success)
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
            }
        }
    }

    fun addMovie(request: com.example.movie_booking_app.data.model.request.AddMovieRequest) {
        viewModelScope.launch {
            try {
                val success = repository.addMovie(request)
                _movieActionSuccess.emit(success)
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
            }
        }
    }
}
