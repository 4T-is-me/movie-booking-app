package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirecting to the Seating Map screen for testing
        startActivity(Intent(this, selectSeatActivity::class.java))
        finish()
    }
}
