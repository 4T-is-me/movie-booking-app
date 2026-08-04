package com.example.movie_booking_app.viewmodel.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.PayrollItem
import com.example.movie_booking_app.data.repository.PayrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context
import com.example.movie_booking_app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ManagerPayrollViewModel @Inject constructor(
    private val payrollRepository: PayrollRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _payrollListState = MutableStateFlow<List<PayrollItem>>(emptyList())
    val payrollListState: StateFlow<List<PayrollItem>> = _payrollListState.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadManagerPayrolls() {
        viewModelScope.launch {
            _isLoading.value = true
            payrollRepository.getManagerPayrolls().collect { result ->
                _isLoading.value = false
                result.onSuccess { response ->
                    _payrollListState.value = response.data ?: emptyList()
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: context.getString(R.string.failed_to_load_manager_payrolls)
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
