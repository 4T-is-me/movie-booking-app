package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.OtpRequest
import com.example.movie_booking_app.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

import com.example.movie_booking_app.R

class ForgotPasswordActivity : EnglishBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val edtEmail = findViewById<EditText>(R.id.edtEmailOtp)
        val btnSend = findViewById<Button>(R.id.btnSendOtp)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        btnSend.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (email.isEmpty()) {
                showAlert("Notification", getString(R.string.please_enter_email))
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            pbLoading.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = OtpRequest(email)
                    val response = RetrofitClient.authApi.sendOtp(request)
                    withContext(Dispatchers.Main) {
                        btnSend.isEnabled = true
                        pbLoading.visibility = View.GONE
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
                        btnSend.isEnabled = true
                        pbLoading.visibility = View.GONE
                        showAlert("Network Error", getString(R.string.connection_error))
                    }
                }
            }
        }
    }

    private fun showAlert(title: String, message: String, onSuccess: (() -> Unit)? = null) {
        val dialog = com.example.movie_booking_app.view.dialog.ErrorDialog(
            context = this,
            title = title,
            message = message,
            onOkClicked = onSuccess
        )
        dialog.show()
    }
}
