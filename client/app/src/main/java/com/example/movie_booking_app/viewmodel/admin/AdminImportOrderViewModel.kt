package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.data.repository.ImportOrderRepository
import com.example.movie_booking_app.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminImportOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<ImportOrder> = emptyList(),
    val selectedStatus: String? = null,
    val error: String? = null,
    val successMessageResId: Int? = null,
    val successMessageArg: String? = null
)

@HiltViewModel
class AdminImportOrderViewModel @Inject constructor(
    private val importOrderRepository: ImportOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminImportOrderUiState())
    val uiState: StateFlow<AdminImportOrderUiState> = _uiState.asStateFlow()

    init {
        loadOrders(Constants.ImportOrderStatus.PENDING)
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
                                successMessageResId = R.string.goods_intake_success_approve,
                                successMessageArg = updatedOrder.importCode
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
                                successMessageResId = R.string.goods_intake_success_reject,
                                successMessageArg = updatedOrder.importCode
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
     }

     fun resetStatus() {
        _uiState.update { it.copy(error = null, successMessageResId = null, successMessageArg = null) }
     }
}
