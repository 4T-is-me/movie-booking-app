package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.OtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val edtEmail = findViewById<EditText>(R.id.edtEmailOtp)
        val btnSend = findViewById<Button>(R.id.btnSendOtp)

        btnSend.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = OtpRequest(email)
                    val response = RetrofitClient.authApi.sendOtp(request)
                    withContext(Dispatchers.Main) {
                        btnSend.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity, "Đã gửi mã OTP", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, "Lỗi gửi OTP", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSend.isEnabled = true
                        Toast.makeText(this@ForgotPasswordActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
