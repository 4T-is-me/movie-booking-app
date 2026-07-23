package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.OtpRequest
import com.example.movie_booking_app.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendOtp.setOnClickListener {
            val email = binding.edtEmailOtp.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSendOtp.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = OtpRequest(email)
                    val response = RetrofitClient.authApi.sendOtp(request)
                    withContext(Dispatchers.Main) {
                        binding.btnSendOtp.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, "Error sending OTP", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnSendOtp.isEnabled = true
                        Toast.makeText(this@ForgotPasswordActivity, "Connection error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
