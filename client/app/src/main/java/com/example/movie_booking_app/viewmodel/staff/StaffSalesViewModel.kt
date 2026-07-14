package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.model.response.MemberVerifyData
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffSalesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository
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

    // Step state: 1 = Movie & Seat Map, 2 = Member Verification, 3 = Promo, 4 = Complete
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Member Verification state
    private val _verifiedMember = MutableStateFlow<MemberVerifyData?>(null)
    val verifiedMember: StateFlow<MemberVerifyData?> = _verifiedMember.asStateFlow()

    private val _memberVerificationError = MutableStateFlow<String?>(null)
    val memberVerificationError: StateFlow<String?> = _memberVerificationError.asStateFlow()

    private val _isVerifyingMember = MutableStateFlow(false)
    val isVerifyingMember: StateFlow<Boolean> = _isVerifyingMember.asStateFlow()

    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie
        _currentStep.value = 1
        _verifiedMember.value = null
        _memberVerificationError.value = null
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

    fun verifyMember(identity: String) {
        if (identity.trim().isEmpty()) {
            _memberVerificationError.value = "Vui lòng nhập thông tin xác thực"
            _verifiedMember.value = null
            return
        }

        viewModelScope.launch {
            _isVerifyingMember.value = true
            _memberVerificationError.value = null
            _verifiedMember.value = null
            try {
                memberRepository.verifyMember(identity).collect { response ->
                    if (response.success && response.data != null) {
                        _verifiedMember.value = response.data
                        _memberVerificationError.value = null
                    } else {
                        _verifiedMember.value = null
                        _memberVerificationError.value = "Tài khoản thành viên không tồn tại"
                    }
                }
            } catch (e: Exception) {
                _verifiedMember.value = null
                _memberVerificationError.value = e.message ?: "Lỗi hệ thống khi xác thực"
            } finally {
                _isVerifyingMember.value = false
            }
        }
    }

    fun clearMemberVerification() {
        _verifiedMember.value = null
        _memberVerificationError.value = null
    }

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }
}
