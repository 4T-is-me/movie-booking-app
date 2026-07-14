package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffSalesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _showtimes = MutableStateFlow<List<Showtime>>(emptyList())
    val showtimes: StateFlow<List<Showtime>> = _showtimes.asStateFlow()

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _selectedSeats = MutableStateFlow<List<Seat>>(emptyList())
    val selectedSeats: StateFlow<List<Seat>> = _selectedSeats.asStateFlow()

    private val _totalPrice = MutableStateFlow(0)
    val totalPrice: StateFlow<Int> = _totalPrice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie
        if (movie != null) {
            loadShowtimes(movie.id ?: "")
        } else {
            _showtimes.value = emptyList()
            _seats.value = emptyList()
            _selectedSeats.value = emptyList()
            _totalPrice.value = 0
        }
    }

    fun loadMovies() {
        viewModelScope.launch {
            _isLoading.value = true
            movieRepository.getMovies().collect { list ->
                _movies.value = list
                _isLoading.value = false
            }
        }
    }

    fun loadShowtimes(movieId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _seats.value = emptyList()
            _selectedSeats.value = emptyList()
            _totalPrice.value = 0
            movieRepository.getMovieShowtimes(movieId).collect { list ->
                _showtimes.value = list
                _isLoading.value = false
            }
        }
    }

    fun loadSeats(showtimeId: String) {
        _showtimes.update { list ->
            list.map { it.copy(isSelected = it.id == showtimeId) }
        }
        viewModelScope.launch {
            _isLoading.value = true
            _selectedSeats.value = emptyList()
            _totalPrice.value = 0
            bookingRepository.getSeats(showtimeId).collect { list ->
                _seats.value = list
                _isLoading.value = false
            }
        }
    }

    fun toggleSeatSelection(seatId: String) {
        val seatToToggle = _seats.value.find { it.id == seatId } ?: return
        if (seatToToggle.displayStatus == SeatStatus.RESERVED || seatToToggle.displayStatus == SeatStatus.HOLDING) return

        _seats.update { list ->
            list.map { currentSeat ->
                if (currentSeat.id == seatId) {
                    val newStatus = if (currentSeat.displayStatus == SeatStatus.SELECTED)
                        SeatStatus.AVAILABLE else SeatStatus.SELECTED
                    currentSeat.copy(displayStatus = newStatus)
                } else {
                    currentSeat
                }
            }
        }
        val selected = _seats.value.filter { it.displayStatus == SeatStatus.SELECTED }
        _selectedSeats.value = selected
        _totalPrice.value = selected.fold(0) { acc, seat -> acc + seat.price }
    }
}
