package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.ResetPasswordRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        email = intent.getStringExtra("email") ?: ""

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        val edtNewPassword = findViewById<TextInputEditText>(R.id.edtNewPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        btnResetPassword.setOnClickListener {
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_password_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.error_confirm_password_mismatch), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnResetPassword.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = ResetPasswordRequest(email, newPassword)
                    val response = RetrofitClient.authApi.resetPassword(request)

                    withContext(Dispatchers.Main) {
                        btnResetPassword.isEnabled = true
                        pbLoading.visibility = View.GONE

                        if (response.isSuccessful) {
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.reset_password_success), Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string()
                                val jsonObject = JSONObject(errorBody ?: "")
                                val message = jsonObject.getString("message")
                                Toast.makeText(this@ResetPasswordActivity, message, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@ResetPasswordActivity, getString(R.string.error_unable_to_update_password), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnResetPassword.isEnabled = true
                        pbLoading.visibility = View.GONE
                        Toast.makeText(this@ResetPasswordActivity, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
