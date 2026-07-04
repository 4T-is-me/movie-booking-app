package com.example.movie_booking_app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manager class for handling authentication tokens using EncryptedSharedPreferences.
 */
object TokenManager {

    private const val PREF_NAME = "encrypted_app_prefs"
    private const val KEY_ACCESS_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val pref = getEncryptedPrefs(context)
        pref.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        val pref = getEncryptedPrefs(context)
        return pref.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        val pref = getEncryptedPrefs(context)
        return pref.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens(context: Context) {
        val pref = getEncryptedPrefs(context)
        pref.edit().clear().apply()
    }
}
