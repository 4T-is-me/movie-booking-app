package com.example.movie_booking_app.view.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * BaseActivity: The parent of all activities in the app.
 * Enforces English language and enables modern Edge-to-Edge display.
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("en")
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Kích hoạt chế độ tràn viền hiện đại
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
}
