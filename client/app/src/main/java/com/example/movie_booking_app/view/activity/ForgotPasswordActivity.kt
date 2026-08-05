package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.OtpRequest
import com.example.movie_booking_app.databinding.ActivityForgotPasswordBinding
import com.example.movie_booking_app.view.dialog.ErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ForgotPasswordActivity : EnglishBaseActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendOtp.setOnClickListener {
            val email = binding.edtEmailOtp.text.toString().trim()
            if (email.isEmpty()) {
                showAlert("Notification", getString(R.string.please_enter_email))
                return@setOnClickListener
            }

            binding.btnSendOtp.isEnabled = false
            binding.pbLoading.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = OtpRequest(email)
                    val response = RetrofitClient.authApi.sendOtp(request)
                    withContext(Dispatchers.Main) {
                        binding.btnSendOtp.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                        if (response.isSuccessful) {
                            val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string()
                                val jsonObject = JSONObject(errorBody ?: "")
                                val message = jsonObject.getString("message")
                                showAlert("Error", message)
                            } catch (e: Exception) {
                                showAlert("Error", getString(R.string.error_sending_otp))
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnSendOtp.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                        showAlert(getString(R.string.title_network_error), getString(R.string.connection_error))
                    }
                }
            }
        }
    }

    private fun showAlert(title: String, message: String, onSuccess: (() -> Unit)? = null) {
        val dialog = ErrorDialog(
            context = this,
            title = title,
            message = message,
            onOkClicked = onSuccess
        )
        dialog.show()
    }
}
