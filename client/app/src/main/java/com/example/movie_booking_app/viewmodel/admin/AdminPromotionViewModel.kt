package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.AdminPromotionRequest
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.example.movie_booking_app.data.repository.admin.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context
import com.example.movie_booking_app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdminPromotionUiState(
    val isLoading: Boolean = false,
    val promotions: List<AdminPromotion> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminPromotionViewModel @Inject constructor(
    private val repository: AdminRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPromotionUiState())
    val uiState: StateFlow<AdminPromotionUiState> = _uiState.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess: SharedFlow<Boolean> = _actionSuccess.asSharedFlow()

    private val _actionMessage = MutableSharedFlow<String>()
    val actionMessage: SharedFlow<String> = _actionMessage.asSharedFlow()

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.getPromotions()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val data = response.body()?.data ?: emptyList()
                            _uiState.value = AdminPromotionUiState(
                                isLoading = false,
                                promotions = data,
                                error = null
                            )
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: context.getString(R.string.failed_to_load_promotions)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: context.getString(R.string.network_connection_error)
                        )
                    }
                }
            }
        }
    }

    fun createPromotion(request: AdminPromotionRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.createPromotion(request)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit(context.getString(R.string.promotion_added_successfully))
                            loadPromotions()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: context.getString(R.string.failed_to_add_promotion))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: context.getString(R.string.network_connection_error))
                    }
                }
            }
        }
    }

    fun updatePromotion(id: String, request: AdminPromotionRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.updatePromotion(id, request)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit(context.getString(R.string.promotion_updated_successfully))
                            loadPromotions()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: context.getString(R.string.failed_to_update_promotion))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: context.getString(R.string.network_connection_error))
                    }
                }
            }
        }
    }

    fun deletePromotion(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.deletePromotion(id)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit(context.getString(R.string.promotion_deleted_successfully))
                            loadPromotions()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: context.getString(R.string.failed_to_delete_promotion))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: context.getString(R.string.network_connection_error))
                    }
                }
            }
        }
    }
}
