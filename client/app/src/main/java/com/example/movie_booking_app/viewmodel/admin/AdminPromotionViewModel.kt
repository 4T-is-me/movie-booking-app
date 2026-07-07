package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.example.movie_booking_app.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AdminPromotionUiState(
    val isLoading: Boolean = false,
    val promotions: List<AdminPromotion> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminPromotionViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPromotionUiState())
    val uiState: StateFlow<AdminPromotionUiState> = _uiState.asStateFlow()

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
                            val errorMsg = response.errorBody()?.string() ?: "Lấy danh sách khuyến mãi thất bại"
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
                            error = e.localizedMessage ?: "Lỗi kết nối mạng"
                        )
                    }
                }
            }
        }
    }
}
