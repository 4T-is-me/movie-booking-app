package com.example.movie_booking_app.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movie_booking_app.databinding.ActivityLoginPlaceholderBinding

class LoginPlaceholderActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginPlaceholderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPlaceholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
