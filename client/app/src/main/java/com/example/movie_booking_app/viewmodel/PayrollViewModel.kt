package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.response.PayrollItem
import com.example.movie_booking_app.data.repository.CinemasRepository
import com.example.movie_booking_app.data.repository.PayrollRepository
import com.example.movie_booking_app.data.repository.manager.ManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val payrollRepository: PayrollRepository,
    private val cinemasRepository: CinemasRepository,
    private val managerRepository: ManagerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _payrollListState = MutableStateFlow<List<PayrollItem>>(emptyList())
    val payrollListState: StateFlow<List<PayrollItem>> = _payrollListState.asStateFlow()

    private val _payrollDetailState = MutableStateFlow<PayrollItem?>(null)
    val payrollDetailState: StateFlow<PayrollItem?> = _payrollDetailState.asStateFlow()

    private val _cinemasState = MutableStateFlow<List<Cinema>>(emptyList())
    val cinemasState: StateFlow<List<Cinema>> = _cinemasState.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _generateResult = MutableStateFlow<String?>(null)
    val generateResult: StateFlow<String?> = _generateResult.asStateFlow()

    private val _payResult = MutableStateFlow<String?>(null)
    val payResult: StateFlow<String?> = _payResult.asStateFlow()

    var userRole: String? = null
        private set
    
    var userCinemaId: String? = null
        private set

    init {
        detectUserRole()
        loadInitialData()
    }

    private fun detectUserRole() {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userRole = sharedPref.getString("role", null)
    }

    fun isAdmin(): Boolean {
        return userRole?.contains("admin", ignoreCase = true) == true
    }

    fun isManager(): Boolean {
        return userRole?.contains("manager", ignoreCase = true) == true
    }

    fun isStaff(): Boolean {
        return userRole?.contains("staff", ignoreCase = true) == true || 
               userRole?.contains("employee", ignoreCase = true) == true
    }

    fun loadInitialData() {
        if (isAdmin()) {
            loadCinemas()
            loadPayrolls()
        } else if (isManager()) {
            fetchManagerCinemaId()
        } else {
            // Staff
            loadPayrolls()
        }
    }

    private fun fetchManagerCinemaId() {
        viewModelScope.launch {
            _isLoading.value = true
            managerRepository.getStaffForManager().collect { response ->
                _isLoading.value = false
                if (response?.success == true && response.data != null) {
                    userCinemaId = response.data.cinemaId
                    loadPayrolls()
                } else {
                    _errorMessage.value = context.getString(R.string.error_fetch_cinema_assignment)
                }
            }
        }
    }

    private fun loadCinemas() {
        viewModelScope.launch {
            try {
                val list = cinemasRepository.getCinemas()
                _cinemasState.value = list
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun loadPayrolls(
        month: Int? = null,
        year: Int? = null,
        cinemaId: String? = null,
        status: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            // Managers can only search their own cinema
            val finalCinemaId = if (isManager()) userCinemaId else cinemaId
            
            payrollRepository.getPayrolls(month, year, finalCinemaId, status).collect { result ->
                _isLoading.value = false
                result.onSuccess { response ->
                    _payrollListState.value = response.data ?: emptyList()
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: context.getString(R.string.error_load_payrolls)
                }
            }
        }
    }

    fun loadPayrollDetail(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            payrollRepository.getPayrollById(id).collect { result ->
                _isLoading.value = false
                result.onSuccess { response ->
                    _payrollDetailState.value = response.data
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: context.getString(R.string.error_load_payroll_details)
                }
            }
        }
    }

    fun generatePayroll(month: Int, year: Int) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            payrollRepository.generatePayroll(month, year).collect { result ->
                _isLoading.value = false
                result.onSuccess { response ->
                    val msg = response.message ?: ""
                    if (msg.contains(" 0 ", ignoreCase = true) || msg.startsWith("0 ")) {
                        _errorMessage.value = context.getString(R.string.error_payroll_already_generated)
                    } else {
                        _generateResult.value = msg.ifEmpty { context.getString(R.string.msg_payroll_generated_success) }
                    }
                }.onFailure { exception ->
                    val rawError = exception.message ?: ""
                    val finalMsg = parseRawError(rawError) ?: rawError

                    if (finalMsg.contains("already generated", ignoreCase = true) ||
                        finalMsg.contains("exists", ignoreCase = true) ||
                        finalMsg.contains("409", ignoreCase = true)) {
                        _errorMessage.value = context.getString(R.string.error_payroll_already_generated)
                    } else {
                        _errorMessage.value = finalMsg.ifEmpty { context.getString(R.string.error_generate_payroll_failed) }
                    }
                }
            }
        }
    }

    fun payPayroll(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            payrollRepository.markAsPaid(id).collect { result ->
                _isLoading.value = false
                result.onSuccess { response ->
                    _payResult.value = response.message ?: context.getString(R.string.msg_payroll_marked_paid)
                    // Reload detail
                    loadPayrollDetail(id)
                }.onFailure { exception ->
                    val rawError = exception.message ?: ""
                    _errorMessage.value = parseRawError(rawError) ?: rawError.ifEmpty { context.getString(R.string.error_update_payment_status) }
                }
            }
        }
    }

    private fun parseRawError(rawError: String): String? {
        return try {
            val json = JSONObject(rawError)
            val errors = json.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                errors.getJSONObject(0).optString("message")
            } else {
                json.optString("message").let { if (it.isEmpty()) null else it }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearActionStates() {
        _generateResult.value = null
        _payResult.value = null
        _errorMessage.value = null
    }
}
