package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.viewmodel.OtpUiState
import com.example.movie_booking_app.viewmodel.OtpViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VerifyOtpActivity : AppCompatActivity() {

    private val viewModel: OtpViewModel by viewModels()
    private lateinit var btnVerify: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var email: String
    private lateinit var type: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        email = intent.getStringExtra("email") ?: ""
        type = intent.getStringExtra("type") ?: "forgot_password"
        val tvMessage = findViewById<TextView>(R.id.tvOtpMessage)
        tvMessage.text = "Nhập mã OTP 6 số đã được gửi đến:\n$email"

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        btnVerify = findViewById(R.id.btnVerifyOtp)
        pbLoading = findViewById(R.id.pbLoading)
        tvError = findViewById(R.id.tvError)

        val otpBoxes = arrayOf(
            findViewById<EditText>(R.id.edtOtp1),
            findViewById<EditText>(R.id.edtOtp2),
            findViewById<EditText>(R.id.edtOtp3),
            findViewById<EditText>(R.id.edtOtp4),
            findViewById<EditText>(R.id.edtOtp5),
            findViewById<EditText>(R.id.edtOtp6)
        )

        setupOtpInputs(otpBoxes)

        btnVerify.setOnClickListener {
            val otpCode = otpBoxes.joinToString("") { it.text.toString() }
            if (otpCode.length != 6) {
                showError("Vui lòng nhập đủ 6 số OTP")
                return@setOnClickListener
            }

            hideError()
            viewModel.verifyOtp(email, otpCode, type)
        }

        observeViewModel()
    }

    private fun setupOtpInputs(boxes: Array<EditText>) {
        for (i in boxes.indices) {
            val currentBox = boxes[i]
            
            currentBox.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hideError()
                    if (s?.length == 1 && i < boxes.size - 1) {
                        boxes[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            currentBox.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (currentBox.text.isEmpty() && i > 0) {
                        boxes[i - 1].requestFocus()
                        boxes[i - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is OtpUiState.Idle -> {
                        btnVerify.isEnabled = true
                        pbLoading.visibility = View.GONE
                    }
                    is OtpUiState.Loading -> {
                        btnVerify.isEnabled = false
                        pbLoading.visibility = View.VISIBLE
                    }
                    is OtpUiState.Success -> {
                        btnVerify.isEnabled = true
                        pbLoading.visibility = View.GONE
                        Toast.makeText(this@VerifyOtpActivity, "Xác thực thành công!", Toast.LENGTH_SHORT).show()
                        
                        if (type == "register") {
                            val intent = Intent(this@VerifyOtpActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@VerifyOtpActivity, ResetPasswordActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        }
                        finish()
                    }
                    is OtpUiState.Error -> {
                        btnVerify.isEnabled = true
                        pbLoading.visibility = View.GONE
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
