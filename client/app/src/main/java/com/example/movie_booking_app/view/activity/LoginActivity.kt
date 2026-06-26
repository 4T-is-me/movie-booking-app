package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var passwordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtError = findViewById(R.id.txtError)
        passwordLayout = findViewById(R.id.passwordLayout)
        
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)
        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            // Handle forgot password
        }

        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)

        // Initialize eye icon state
        edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off)

        passwordLayout.setEndIconOnClickListener {
            val selection = edtPassword.selectionEnd
            if (edtPassword.transformationMethod is PasswordTransformationMethod) {
                edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            edtPassword.setSelection(selection)
        }

        // Hide error when user starts typing
        edtUsername.addTextChangedListener { hideError() }
        edtPassword.addTextChangedListener { hideError() }

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty()) {
                showError("Please enter username")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Please enter password")
                return@setOnClickListener
            }

            Log.d("LOGIN_DEBUG", "Username: $username")

            btnLogin.isEnabled = false
            hideError()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = loginRequest(username, password)
                    val response = RetrofitClient.authApi.login(request)

                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val loginData = response.body()!!

                            
                            // Save tokens
                            TokenManager.saveTokens(
                                this@LoginActivity,
                                loginData.accessToken,
                                loginData.refreshToken
                            )

                            // Get saved tokens
                            val savedAccessToken = TokenManager.getAccessToken(this@LoginActivity)
                            val savedRefreshToken = TokenManager.getRefreshToken(this@LoginActivity)

                            Log.d("TOKEN_DEBUG", " Access Tokennn: $savedAccessToken")
                            Log.d("TOKEN_DEBUG", " Refresh Tokennn $savedRefreshToken")

//                            Toast.makeText(this@LoginActivity, "Login successful! Token saved.", Toast.LENGTH_SHORT).show()



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
        txtError.visibility = View.GONE
    }
}
