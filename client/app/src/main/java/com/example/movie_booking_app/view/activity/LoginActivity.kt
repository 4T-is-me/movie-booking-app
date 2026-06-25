package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.request.loginRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var txtError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtError = findViewById(R.id.txtError)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)
        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)

        var isPasswordVisible = false
        passwordLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {

                edtPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility)
            } else {

                edtPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            // Đưa con trỏ văn bản về cuối cùng
            edtPassword.setSelection(edtPassword.text?.length ?: 0)
        }

        // Hide error when user starts typing
        edtUsername.addTextChangedListener { hideError() }
        edtPassword.addTextChangedListener { hideError() }

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Please enter both username and password")
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            hideError()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = loginRequest(username, password)
                    val response = RetrofitClient.authApi.login(request)

                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val accessToken = response.body()?.accessToken
                            if (accessToken == null) {
                                Log.e("TOKEN_DEBUG", "❌ CHƯA CÓ TOKEN")
                            } else {
                                Log.d("TOKEN_DEBUG", "✅ TOKEN OK: $accessToken")
                            }

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)

                            finish()
                        } else {
                            val errorMsg = when(response.code()) {
                                401 -> "Invalid username or password"
                                404 -> "Account not found"
                                else -> "Login failed (Error: ${response.code()})"
                            }
                            showError(errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        showError("Connection error: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = View.VISIBLE
    }

    private fun hideError() {
        if (txtError.visibility == View.VISIBLE) {
            txtError.visibility = View.GONE
        }
    }
}
