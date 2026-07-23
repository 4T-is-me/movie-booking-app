package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.local.NotificationItem
import com.example.movie_booking_app.data.local.NotificationLocalStorage
import com.example.movie_booking_app.data.model.response.NotificationItemResponse
import com.example.movie_booking_app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItemResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationItemResponse>> = _notifications.asStateFlow()

    private val _mergedNotifications = MutableStateFlow<List<NotificationItemResponse>>(emptyList())
    val mergedNotifications: StateFlow<List<NotificationItemResponse>> = _mergedNotifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getNotifications().collect { response ->
                _isLoading.value = false
                if (response?.success == true) {
                    _notifications.value = response.data ?: emptyList()
                    _unreadCount.value = response.unreadCount ?: 0
                }
            }
        }
    }

    fun fetchStaffNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            val localList = NotificationLocalStorage.getAll(context)
            repository.getStaffNotifications().collect { response ->
                _isLoading.value = false
                val apiList = response?.data ?: emptyList()
                val merged = mergeNotifications(apiList, localList)
                _mergedNotifications.value = merged
                _unreadCount.value = merged.count { !it.isRead }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            val localId = id.toLongOrNull()
            if (localId != null) {
                NotificationLocalStorage.markRead(context, localId)
                fetchStaffNotifications()
            } else {
                repository.markAsRead(id).collect { response ->
                    if (response?.success == true) {
                        fetchStaffNotifications()
                    }
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            NotificationLocalStorage.markAllRead(context)
            repository.markAllAsRead().collect { response ->
                if (response?.success == true) {
                    fetchStaffNotifications()
                }
            }
            fetchStaffNotifications()
        }
    }

    private fun mergeNotifications(
        apiList: List<NotificationItemResponse>,
        localList: List<NotificationItem>
    ): List<NotificationItemResponse> {
        val mappedLocal = localList.map { mapLocalToResponse(it) }
        val combined = apiList + mappedLocal

        // Sort descending by time
        val sorted = combined.sortedByDescending { parseIsoToLong(it.createdAt) }

        // Remove duplicates (by id, or by referenceId if not null)
        val result = mutableListOf<NotificationItemResponse>()
        val seenIds = mutableSetOf<String>()
        val seenRefIds = mutableSetOf<String>()

        for (item in sorted) {
            val refId = item.metadata?.get("referenceId")?.toString()

            if (seenIds.contains(item.id)) {
                continue
            }
            if (!refId.isNullOrEmpty() && seenRefIds.contains(refId)) {
                continue
            }

            seenIds.add(item.id)
            if (!refId.isNullOrEmpty()) {
                seenRefIds.add(refId)
            }
            result.add(item)
        }
        return result
    }

    private fun mapLocalToResponse(item: NotificationItem): NotificationItemResponse {
        val metadataMap = mutableMapOf<String, Any>()
        item.referenceId?.let { metadataMap["referenceId"] = it }
        item.shiftId?.let { metadataMap["shiftId"] = it }
        item.leaveRequestId?.let { metadataMap["leaveRequestId"] = it }

        return NotificationItemResponse(
            id = item.id.toString(),
            recipientId = "",
            title = item.title,
            message = item.body,
            isRead = item.isRead,
            type = item.type,
            metadata = metadataMap,
            createdAt = formatLongToIso(item.timestamp)
        )
    }

    private fun formatLongToIso(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    private fun parseIsoToLong(createdAt: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(createdAt)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf2.parse(createdAt)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }
}