package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.data.repository.ImportOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminImportOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<ImportOrder> = emptyList(),
    val selectedStatus: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AdminImportOrderViewModel @Inject constructor(
    private val importOrderRepository: ImportOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminImportOrderUiState())
    val uiState: StateFlow<AdminImportOrderUiState> = _uiState.asStateFlow()

    init {
        loadOrders("PENDING")
    }

    fun loadOrders(status: String? = null, cinemaId: String? = null) {
        viewModelScope.launch {
            importOrderRepository.getAdminImportOrders(status, cinemaId)
                .onStart { _uiState.update { it.copy(isLoading = true, selectedStatus = status) } }
                .collect { result ->
                    result.onSuccess { list ->
                        _uiState.update { it.copy(orders = list, isLoading = false) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun approveOrder(orderId: String) {
        viewModelScope.launch {
            importOrderRepository.approveImportOrder(orderId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { updatedOrder ->
                        val updatedList = _uiState.value.orders.map {
                            if (it.id == updatedOrder.id) updatedOrder else it
                        }
                        _uiState.update {
                            it.copy(
                                orders = updatedList,
                                isLoading = false,
                                successMessage = "Approved order ${updatedOrder.importCode} and updated stock successfully"
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            importOrderRepository.rejectImportOrder(orderId, reason)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { updatedOrder ->
                        val updatedList = _uiState.value.orders.map {
                            if (it.id == updatedOrder.id) updatedOrder else it
                        }
                        _uiState.update {
                            it.copy(
                                orders = updatedList,
                                isLoading = false,
                                successMessage = "Rejected order ${updatedOrder.importCode}"
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun resetStatus() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
