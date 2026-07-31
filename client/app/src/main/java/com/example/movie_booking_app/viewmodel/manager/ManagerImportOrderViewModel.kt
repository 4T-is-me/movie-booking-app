package com.example.movie_booking_app.viewmodel.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.api.CreateImportOrderRequest
import com.example.movie_booking_app.data.api.ImportOrderItemRequest
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.data.model.entity.Supplier
import com.example.movie_booking_app.data.repository.ImportOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagerImportOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<ImportOrder> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val inventoryItems: List<Inventory> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ManagerImportOrderViewModel @Inject constructor(
    private val importOrderRepository: ImportOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerImportOrderUiState())
    val uiState: StateFlow<ManagerImportOrderUiState> = _uiState.asStateFlow()

    init {
        loadOrders("PENDING")
        loadSuppliers()
        loadManagerInventory()
    }

    fun loadOrders(status: String? = null) {
        viewModelScope.launch {
            importOrderRepository.getManagerImportOrders(status)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { list ->
                        _uiState.update { it.copy(orders = list, isLoading = false) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            importOrderRepository.getSuppliers()
                .collect { result ->
                    result.onSuccess { list ->
                        _uiState.update { it.copy(suppliers = list) }
                    }
                }
        }
    }

    fun loadManagerInventory() {
        viewModelScope.launch {
            importOrderRepository.getManagerInventory()
                .collect { result ->
                    result.onSuccess { list ->
                        _uiState.update { it.copy(inventoryItems = list) }
                    }
                }
        }
    }

    fun createImportOrder(
        supplierId: String,
        items: List<ImportOrderItemRequest>,
        notes: String?
    ) {
        viewModelScope.launch {
            val request = CreateImportOrderRequest(supplierId, items, notes)
            importOrderRepository.createImportOrder(request)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { order ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                orders = listOf(order) + it.orders,
                                successMessage = "Import order ${order.importCode} created successfully"
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun cancelImportOrder(orderId: String) {
        viewModelScope.launch {
            importOrderRepository.cancelImportOrder(orderId)
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
                                successMessage = "Import order ${updatedOrder.importCode} cancelled"
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
