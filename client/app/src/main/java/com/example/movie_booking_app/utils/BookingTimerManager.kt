package com.example.movie_booking_app.utils

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class BookingTimerManager @Inject constructor() {
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTimer(seconds: Int) {
        stopTimer()
        _remainingSeconds.value = seconds
        _isExpired.value = false
        _isActive.value = true
        
        timerJob = scope.launch {
            var t = seconds
            while (t > 0) {
                delay(1000)
                t--
                _remainingSeconds.value = t
            }
            _isExpired.value = true
            _isActive.value = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _remainingSeconds.value = 0
    }
}
