package com.example.movie_booking_app.viewmodel.staff

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.model.response.MemberVerifyData
import com.example.movie_booking_app.data.model.response.PromotionData
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.MemberRepository
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

@HiltViewModel
class StaffSalesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
    private val movieRepository: MovieRepository,
    private val bookingRepository: BookingRepository,
    private val memberRepository: MemberRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _allShowtimes = MutableStateFlow<List<Showtime>>(emptyList())

    private val _dates = MutableStateFlow<List<ShowDate>>(emptyList())
    val dates: StateFlow<List<ShowDate>> = _dates.asStateFlow()

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

    // Promotion states
    private val _availablePromotions = MutableStateFlow<List<Promo>>(emptyList())
    val availablePromotions: StateFlow<List<Promo>> = _availablePromotions.asStateFlow()

    private val _appliedPromotion = MutableStateFlow<PromotionData?>(null)
    val appliedPromotion: StateFlow<PromotionData?> = _appliedPromotion.asStateFlow()

    private val _useScore = MutableStateFlow(false)
    val useScore: StateFlow<Boolean> = _useScore.asStateFlow()

    private val _scoreToRedeem = MutableStateFlow(0)
    val scoreToRedeem: StateFlow<Int> = _scoreToRedeem.asStateFlow()

    private val _scoreError = MutableStateFlow<String?>(null)
    val scoreError: StateFlow<String?> = _scoreError.asStateFlow()

    private val _promoDiscount = MutableStateFlow(0)
    val promoDiscount: StateFlow<Int> = _promoDiscount.asStateFlow()

    private val _pointDiscount = MutableStateFlow(0)
    val pointDiscount: StateFlow<Int> = _pointDiscount.asStateFlow()

    private val _grandTotal = MutableStateFlow(0)
    val grandTotal: StateFlow<Int> = _grandTotal.asStateFlow()

    private val _promoError = MutableStateFlow<String?>(null)
    val promoError: StateFlow<String?> = _promoError.asStateFlow()

    private val _promoSuccessMessage = MutableStateFlow<String?>(null)
    val promoSuccessMessage: StateFlow<String?> = _promoSuccessMessage.asStateFlow()

    private val _isApplyingPromo = MutableStateFlow(false)
    val isApplyingPromo: StateFlow<Boolean> = _isApplyingPromo.asStateFlow()

    // Checkout states
    private val _checkoutSuccess = MutableStateFlow(false)
    val checkoutSuccess: StateFlow<Boolean> = _checkoutSuccess.asStateFlow()

    private val _checkoutCode = MutableStateFlow<String?>(null)
    val checkoutCode: StateFlow<String?> = _checkoutCode.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    private val _isCheckingOut = MutableStateFlow(false)
    val isCheckingOut: StateFlow<Boolean> = _isCheckingOut.asStateFlow()

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
        _appliedPromotion.value = null
        _useScore.value = false
        _scoreToRedeem.value = 0
        _scoreError.value = null
        _promoDiscount.value = 0
        _pointDiscount.value = 0
        _grandTotal.value = 0
        _promoError.value = null
        _promoSuccessMessage.value = null
        _checkoutSuccess.value = false
        _checkoutCode.value = null
        _checkoutError.value = null
        if (movie != null) {
            loadShowtimes(movie.id ?: "")
        } else {
            _allShowtimes.value = emptyList()
            _dates.value = emptyList()
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
            _allShowtimes.value = emptyList()
            _dates.value = emptyList()
            _showtimes.value = emptyList()
            _seats.value = emptyList()
            _selectedSeats.value = emptyList()
            _totalPrice.value = 0
            _appliedPromotion.value = null
            _useScore.value = false
            _scoreToRedeem.value = 0
            _scoreError.value = null
            recalculateTotals()
            movieRepository.getMovieShowtimes(movieId).collect { list ->
                _allShowtimes.value = list
                
                val dateMap = mutableMapOf<String, ShowDate>()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val dowFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

                list.forEach { showtime ->
                    val dateStr = showtime.showDate?.take(10) ?: return@forEach
                    if (!dateMap.containsKey(dateStr)) {
                        val dateObj = try { isoFormat.parse(dateStr) } catch (e: Exception) { null }
                        if (dateObj != null) {
                            dateMap[dateStr] = ShowDate(
                                id = dateStr,
                                day = dayFormat.format(dateObj),
                                month = monthFormat.format(dateObj),
                                dayOfWeek = dowFormat.format(dateObj),
                                year = yearFormat.format(dateObj),
                                isSelected = false
                            )
                        }
                    }
                }

                val sortedDates = dateMap.values.toList().sortedBy { it.id }
                _dates.value = sortedDates
                
                if (sortedDates.isNotEmpty()) {
                    selectDate(sortedDates[0].id)
                } else {
                    _showtimes.value = emptyList()
                    _isLoading.value = false
                }
            }
        }
    }

    fun selectDate(dateId: String) {
        _dates.update { list -> list.map { it.copy(isSelected = it.id == dateId) } }
        
        val filteredTimes = _allShowtimes.value.filter { it.showDate?.startsWith(dateId) == true }
        _showtimes.value = filteredTimes.map { showtime ->
            showtime.copy(isSelected = false)
        }
        
        if (filteredTimes.isNotEmpty()) {
            loadSeats(filteredTimes[0].id)
        } else {
            _seats.value = emptyList()
            _selectedSeats.value = emptyList()
            _totalPrice.value = 0
            _appliedPromotion.value = null
            _useScore.value = false
            _scoreToRedeem.value = 0
            _scoreError.value = null
            recalculateTotals()
            _isLoading.value = false
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
            _appliedPromotion.value = null
            _useScore.value = false
            _scoreToRedeem.value = 0
            _scoreError.value = null
            recalculateTotals()
            bookingRepository.getSeats(showtimeId).collect { seatMapData ->
                _seats.value = seatMapData.seats ?: emptyList()
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
        recalculateTotals()
    }

    fun verifyMember(identity: String) {
        if (identity.trim().isEmpty()) {
            _memberVerificationError.value = context.getString(R.string.auto_str_please_enter_verification_info)
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
                        _memberVerificationError.value = context.getString(R.string.auto_str_member_account_does_not_exist)
                    }
                    _useScore.value = false
                    recalculateTotals()
                }
            } catch (e: Exception) {
                _verifiedMember.value = null
                _memberVerificationError.value = e.message ?: "System error during verification"
                _useScore.value = false
                recalculateTotals()
            } finally {
                _isVerifyingMember.value = false
            }
        }
    }

    fun clearMemberVerification() {
        _verifiedMember.value = null
        _memberVerificationError.value = null
        _useScore.value = false
        recalculateTotals()
    }

    fun recalculateTotals() {
        val originalPrice = _totalPrice.value
        var currentPromoDiscount = 0
        val promo = _appliedPromotion.value

        if (promo != null) {
            currentPromoDiscount = when (promo.discountType) {
                "percentage" -> {
                    val discountVal = promo.discountValue ?: 0.0
                    Math.floor(originalPrice * (discountVal / 100.0)).toInt()
                }
                "fixed" -> {
                    (promo.discountValue ?: 0.0).toInt()
                }
                else -> {
                    Math.floor(originalPrice * (promo.discountLevel / 100.0)).toInt()
                }
            }
        }

        if (currentPromoDiscount > originalPrice) {
            currentPromoDiscount = originalPrice
        }
        _promoDiscount.value = currentPromoDiscount

        val afterPromo = originalPrice - currentPromoDiscount

        var currentPointDiscount = 0
        if (_useScore.value) {
            currentPointDiscount = _scoreToRedeem.value * 10
        }

        if (currentPointDiscount > afterPromo) {
            currentPointDiscount = afterPromo
        }
        _pointDiscount.value = currentPointDiscount

        _grandTotal.value = afterPromo - currentPointDiscount
    }

    fun setScoreToRedeem(score: Int) {
        val member = _verifiedMember.value
        if (member != null) {
            if (score > member.currentScore) {
                _scoreError.value = "Insufficient reward points (Available: ${member.currentScore} pts)"
            } else {
                _scoreError.value = null
            }
        } else {
            _scoreError.value = null
        }
        _scoreToRedeem.value = score
        recalculateTotals()
    }

    fun loadAvailablePromotions() {
        viewModelScope.launch {
            _isLoading.value = true
            movieRepository.getPromos().collect { list ->
                _availablePromotions.value = list
                _isLoading.value = false
            }
        }
    }

    fun applyPromoCode(code: String) {
        if (code.trim().isEmpty()) {
            _promoError.value = context.getString(R.string.auto_str_please_enter_promotion_code)
            return
        }

        viewModelScope.launch {
            _isApplyingPromo.value = true
            _promoError.value = null
            _promoSuccessMessage.value = null

            paymentRepository.applyPromotion(code)
                .catch { exception ->
                    _isApplyingPromo.value = false
                    _promoError.value = exception.message ?: "Invalid or expired promotion code"
                    _appliedPromotion.value = null
                    recalculateTotals()
                }
                .collect { response ->
                    _isApplyingPromo.value = false
                    if (response.success && response.data != null) {
                        _appliedPromotion.value = response.data
                        _promoSuccessMessage.value = response.message ?: "Promotion applied successfully!"
                        _promoError.value = null
                    } else {
                        _appliedPromotion.value = null
                        _promoError.value = response.message ?: "Invalid coupon code"
                    }
                    recalculateTotals()
                }
        }
    }

    fun removePromo() {
        _appliedPromotion.value = null
        _promoError.value = null
        _promoSuccessMessage.value = null
        recalculateTotals()
    }

    fun toggleUseScore(use: Boolean) {
        _useScore.value = use
        if (use) {
            val originalPrice = _totalPrice.value
            var currentPromoDiscount = 0
            val promo = _appliedPromotion.value

            if (promo != null) {
                currentPromoDiscount = when (promo.discountType) {
                    "percentage" -> {
                        val discountVal = promo.discountValue ?: 0.0
                        Math.floor(originalPrice * (discountVal / 100.0)).toInt()
                    }
                    "fixed" -> {
                        (promo.discountValue ?: 0.0).toInt()
                    }
                    else -> {
                        Math.floor(originalPrice * (promo.discountLevel / 100.0)).toInt()
                    }
                }
            }
            if (currentPromoDiscount > originalPrice) {
                currentPromoDiscount = originalPrice
            }
            val afterPromo = originalPrice - currentPromoDiscount
            val maxPointsNeeded = Math.ceil(afterPromo / 10.0).toInt()
            val defaultScore = Math.min(_verifiedMember.value?.currentScore ?: 0, maxPointsNeeded)
            setScoreToRedeem(defaultScore)
        } else {
            setScoreToRedeem(0)
        }
    }

    fun confirmBooking(paymentMethod: String) {
        val showtime = _showtimes.value.find { it.isSelected }
        if (showtime == null) {
            _checkoutError.value = context.getString(R.string.auto_str_please_select_a_showtime)
            return
        }

        val selectedSeatsList = _selectedSeats.value
        if (selectedSeatsList.isEmpty()) {
            _checkoutError.value = context.getString(R.string.auto_str_please_select_a_seat)
            return
        }

        viewModelScope.launch {
            _isCheckingOut.value = true
            _checkoutError.value = null
            _checkoutSuccess.value = false

            // Hold the seats first as required by confirmBooking API (requires seat is in hold state)
            val seatIds = selectedSeatsList.map { it.id }
            bookingRepository.holdSeats(showtime.id, seatIds)
                .catch { exception ->
                    _isCheckingOut.value = false
                    _checkoutError.value = "Hold seat failed: ${exception.message}"
                }
                .collect {
                    // Seats held successfully, proceed with booking checkout
                    val request = ConfirmBookingRequest(
                        showtimeId = showtime.id,
                        seatIds = seatIds,
                        useScore = _useScore.value,
                        useRewardPoints = _useScore.value,
                        paymentMethod = paymentMethod,
                        promotionId = _appliedPromotion.value?.id,
                        combos = emptyList(), // Staff tickets sales combo not added in this flow
                        scoreToRedeem = if (_useScore.value) _scoreToRedeem.value else null,
                        memberId = _verifiedMember.value?.memberId,
                        isGuestCheckout = _verifiedMember.value == null
                    )

                    paymentRepository.confirmBooking(request)
                        .catch { checkoutException ->
                            _isCheckingOut.value = false
                            _checkoutError.value = checkoutException.message ?: "Checkout failed"
                            // Release held seats on failure
                            bookingRepository.releaseSeats(showtime.id, seatIds).collectLatest {}
                        }
                        .collect { responseBody ->
                            _isCheckingOut.value = false
                            try {
                                val responseString = responseBody.string()
                                val jsonObject = JSONObject(responseString)
                                val success = jsonObject.optBoolean("success", false)
                                if (success) {
                                    val data = jsonObject.optJSONObject("data")
                                    val bookingCode = data?.optString("bookingCode") ?: "N/A"
                                    _checkoutCode.value = bookingCode
                                    _checkoutSuccess.value = true
                                } else {
                                    val message = jsonObject.optString("message", "Checkout failed")
                                    _checkoutError.value = message
                                    bookingRepository.releaseSeats(showtime.id, seatIds).collectLatest {}
                                }
                            } catch (e: Exception) {
                                _checkoutSuccess.value = false
                                _checkoutError.value = "Response format error: ${e.message}"
                                bookingRepository.releaseSeats(showtime.id, seatIds).collectLatest {}
                            }
                        }
                }
        }
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
