package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.VerifyOtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyOtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        val email = intent.getStringExtra("email") ?: ""
        val tvMessage = findViewById<TextView>(R.id.tvOtpMessage)
        tvMessage.text = "Nhập mã OTP 6 số đã được gửi đến: $email"

        val edtOtp = findViewById<EditText>(R.id.edtOtpCode)
        val btnVerify = findViewById<Button>(R.id.btnVerifyOtp)

        btnVerify.setOnClickListener {
            val otpCode = edtOtp.text.toString().trim()
            if (otpCode.length != 6) {
                Toast.makeText(this, "OTP phải có 6 số", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnVerify.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = VerifyOtpRequest(email, otpCode)
                    val response = RetrofitClient.authApi.verifyOtp(request)
                    withContext(Dispatchers.Main) {
                        btnVerify.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@VerifyOtpActivity, "Xác thực thành công!", Toast.LENGTH_SHORT).show()
                            // TODO: Chuyển sang màn hình Đổi Mật Khẩu
                            // val intent = Intent(this@VerifyOtpActivity, ResetPasswordActivity::class.java)
                            // startActivity(intent)
                        } else {
                            Toast.makeText(this@VerifyOtpActivity, "Mã OTP sai hoặc đã hết hạn", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnVerify.isEnabled = true
                        Toast.makeText(this@VerifyOtpActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
