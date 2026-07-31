package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.data.repository.FoodRepository
import com.example.movie_booking_app.utils.BookingTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

data class FoodUiState(
    val menuItems: List<FoodBeverage> = emptyList(),
    val filteredItems: List<FoodBeverage> = emptyList(),
    val isLoading: Boolean = false,
    val totalFBPrice: Int = 0,
    val selectedCategory: String = "All",
    val countdownText: String = "",
    val isExpired: Boolean = false
)

@HiltViewModel
class SelectFoodViewModel @Inject constructor(
    private val repository: FoodRepository,
    val timerManager: BookingTimerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodUiState())
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    private var currentShowtimeId: String? = null

    fun initBooking(showtimeId: String?) {
        currentShowtimeId = showtimeId
        loadMenu()
        observeTimer()
    }

    private fun loadMenu() {
        viewModelScope.launch {
            repository.getMenu(showtimeId = currentShowtimeId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { items ->
                    val availableItems = items.filter { it.stock > 0 }
                    _uiState.update { 
                        it.copy(
                            menuItems = availableItems,
                            filteredItems = availableItems,
                            isLoading = false
                        ) 
                    }
                }
        }
    }

    private fun observeTimer() {
        viewModelScope.launch {
            timerManager.remainingSeconds.collect { seconds ->
                val minutes = seconds / 60
                val secs = seconds % 60
                _uiState.update { it.copy(countdownText = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)) }
            }
        }
        viewModelScope.launch {
            timerManager.isExpired.collect { expired ->
                if (expired) {
                    _uiState.update { it.copy(isExpired = true) }
                }
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "All") {
                state.menuItems
            } else {
                state.menuItems.filter { it.category.equals(category, ignoreCase = true) }
            }
            state.copy(selectedCategory = category, filteredItems = filtered)
        }
    }

    fun updateQuantity(item: FoodBeverage) {
        // Find item in master list and update
        val updatedList = _uiState.value.menuItems.map {
            if (it.id == item.id) it.copy(selectedQuantity = item.selectedQuantity) else it
        }
        
        val totalPrice = updatedList.sumOf { it.price * it.selectedQuantity }
        
        _uiState.update { state ->
            val filtered = if (state.selectedCategory == "All") {
                updatedList
            } else {
                updatedList.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
            }
            state.copy(menuItems = updatedList, filteredItems = filtered, totalFBPrice = totalPrice)
        }
    }

    fun getSelectedItems(): List<FoodBeverage> {
        return _uiState.value.menuItems.filter { it.selectedQuantity > 0 }
    }
}
