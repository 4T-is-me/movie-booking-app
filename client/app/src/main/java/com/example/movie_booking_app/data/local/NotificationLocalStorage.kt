package com.example.movie_booking_app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Local storage for push notification history using SharedPreferences + Gson.
 *
 * Stores a capped list (max [MAX_ITEMS]) of [NotificationItem] entries as a JSON array.
 * Follows the singleton pattern used by [TokenManager].
 */
object NotificationLocalStorage {

    private const val PREF_NAME   = "notification_history"
    private const val KEY_LIST    = "notifications"
    private const val MAX_ITEMS   = 50

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "guest") ?: "guest"
        return context.getSharedPreferences("${PREF_NAME}_$username", Context.MODE_PRIVATE)
    }

    // ─────────────────────────── WRITE ───────────────────────────

    /**
     * Prepends [item] to the stored list.
     * If the list already has [MAX_ITEMS] entries the oldest is dropped.
     */
    fun save(context: Context, item: NotificationItem) {
        val current = getAll(context).toMutableList()
        current.add(0, item)           // newest first
        if (current.size > MAX_ITEMS) {
            current.subList(MAX_ITEMS, current.size).clear()
        }
        persist(context, current)
    }

    /**
     * Marks the notification identified by [id] as read.
     */
    fun markRead(context: Context, id: Long) {
        val updated = getAll(context).map { if (it.id == id) it.copy(isRead = true) else it }
        persist(context, updated)
    }

    /**
     * Marks all stored notifications as read.
     */
    fun markAllRead(context: Context) {
        val updated = getAll(context).map { it.copy(isRead = true) }
        persist(context, updated)
    }

    /**
     * Removes the notification identified by [id].
     */
    fun delete(context: Context, id: Long) {
        val updated = getAll(context).filter { it.id != id }
        persist(context, updated)
    }

    /**
     * Removes all stored notifications.
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_LIST).apply()
    }

    // ─────────────────────────── READ ────────────────────────────

    /**
     * Returns all stored notifications, newest first.
     */
    fun getAll(context: Context): List<NotificationItem> {
        val json = getPrefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<NotificationItem>>() {}.type
            gson.fromJson<List<NotificationItem>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * Returns the number of unread notifications.
     */
    fun unreadCount(context: Context): Int = getAll(context).count { !it.isRead }

    // ──────────────────────────── PRIVATE ─────────────────────────

    private fun persist(context: Context, list: List<NotificationItem>) {
        getPrefs(context).edit().putString(KEY_LIST, gson.toJson(list)).apply()
    }
}
