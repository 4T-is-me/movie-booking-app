package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.api.RefillRequest
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.data.repository.CinemasRepository
import com.example.movie_booking_app.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminInventoryUiState(
    val cinemas: List<Cinema> = emptyList(),
    val selectedCinema: Cinema? = null,
    val inventoryItems: List<Inventory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val refillSuccess: String? = null
)

@HiltViewModel
class AdminInventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val cinemasRepository: CinemasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminInventoryUiState())
    val uiState: StateFlow<AdminInventoryUiState> = _uiState.asStateFlow()

    init {
        loadCinemas()
    }

    fun loadCinemas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = cinemasRepository.getCinemas()
                _uiState.update { it.copy(cinemas = list, isLoading = false) }
                if (list.isNotEmpty()) {
                    selectCinema(list[0])
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectCinema(cinema: Cinema) {
        _uiState.update { it.copy(selectedCinema = cinema) }
        cinema.id?.let { loadInventory(it) }
    }

    private fun loadInventory(cinemaId: String) {
        viewModelScope.launch {
            inventoryRepository.getInventory(cinemaId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { list ->
                        _uiState.update { it.copy(inventoryItems = list, isLoading = false) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun refill(itemCode: String, quantity: Int, reason: String?) {
        val cinemaId = _uiState.value.selectedCinema?.id ?: return
        
        viewModelScope.launch {
            val request = RefillRequest(cinemaId, itemCode, quantity, reason)
            inventoryRepository.refill(request)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { result ->
                    result.onSuccess { updatedItem ->
                        val updatedList = _uiState.value.inventoryItems.map {
                            if (it.itemCode == updatedItem.itemCode) updatedItem else it
                        }
                        _uiState.update { 
                            it.copy(
                                inventoryItems = updatedList, 
                                isLoading = false,
                                refillSuccess = "Refilled ${updatedItem.itemName} successfully"
                            ) 
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                }
        }
    }

    fun resetStatus() {
        _uiState.update { it.copy(error = null, refillSuccess = null) }
    }
}
