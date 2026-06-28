package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirecting to the Seating Map screen for testing
        startActivity(Intent(this, SelectSeatActivity::class.java))
        finish()
    }
}