package com.example.movie_booking_app.data.local

import android.content.Context

object TokenManager {

    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"

    fun saveToken(context: Context, token: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(KEY_TOKEN, null)
    }

    fun clearToken(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().clear().apply()
    }
}