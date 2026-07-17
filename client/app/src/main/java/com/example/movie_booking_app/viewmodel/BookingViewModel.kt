package com.example.movie_booking_app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Member
import com.example.movie_booking_app.data.model.entity.ScoreHistory
import com.example.movie_booking_app.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class BookingViewModel @Inject constructor(private val repository: BookingRepository): ViewModel() {
    //state score history
    private val _scoreHistoryState = MutableStateFlow<List<ScoreHistory>>(emptyList())
    val scoreHistoryState: StateFlow<List<ScoreHistory>> = _scoreHistoryState.asStateFlow()
    //state progress
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    //state error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _memberState = MutableStateFlow<Member?>(null)
    val memberState: StateFlow<Member?> = _memberState.asStateFlow()

    fun fetchScoreHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.getScoreHistory()
                .catch { exception ->
                    _errorMessage.value = exception.message
                    _isLoading.value = false
                }
                .collect { baseResponse ->
                    val historyList = baseResponse.data

                    // Nếu history có dữ liệu, lấy thông tin member từ giao dịch mới nhất
                    if (historyList.isNotEmpty()) {
                        val latestTransaction = historyList.first()
                        _memberState.value = latestTransaction.memberId.copy(
                            rewardPoints = latestTransaction.remainingPoints
                        )
                    }

                    _scoreHistoryState.value = historyList
                    _isLoading.value = false
                }
        }
    }

}