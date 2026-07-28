package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.PayrollConfig
import com.example.movie_booking_app.data.model.response.UpdatePayrollConfigRequest
import com.example.movie_booking_app.data.repository.PayrollConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayrollConfigViewModel @Inject constructor(private val repository: PayrollConfigRepository) : ViewModel() {
    private val _config = MutableStateFlow<PayrollConfig?>(null)
    val config: StateFlow<PayrollConfig?> = _config.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    fun loadPayrollConfig() = viewModelScope.launch {
        _loading.value = true
        repository.getPayrollConfig().collect { result ->
            _loading.value = false
            result.onSuccess { _config.value = it.data }.onFailure { _message.value = it.message }
        }
    }

    fun updatePayrollConfig(rates: Map<String, Double>, multiplier: Double, bonus: Double) = viewModelScope.launch {
        _loading.value = true
        repository.updateConfig(UpdatePayrollConfigRequest(rates, multiplier, bonus)).collect { result ->
            _loading.value = false
            result.onSuccess { response ->
                _config.value = response.data
                _message.value = response.message
                _updateSuccess.value = true
            }.onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() { 
        _message.value = null
        _updateSuccess.value = false
    }
}
