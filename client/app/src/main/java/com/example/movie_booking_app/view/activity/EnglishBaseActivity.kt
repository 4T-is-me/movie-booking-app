package com.example.movie_booking_app.view.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * EnglishBaseActivity: Base activity for Auth screens that forces English language.
 */
open class EnglishBaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
}
