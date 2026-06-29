package com.example.movie_booking_app.data.local

import android.content.Context

object TokenManager {

    private const val prefName = "app_prefs"
    private const val keyAccessToken = "auth_token"
    private const val keyRefreshToken = "refresh_token"


    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        pref.edit()
            .putString(keyAccessToken, accessToken)
            .putString(keyRefreshToken, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        return pref.getString(keyAccessToken, null)
    }

    fun getRefreshToken(context: Context): String? {
        val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        return pref.getString(keyRefreshToken, null)
    }

    fun clearTokens(context: Context) {
        val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        pref.edit().clear().apply()
    }
}
