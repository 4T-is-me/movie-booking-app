package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.data.local.FcmTokenHelper
import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.databinding.ActivityLoginBinding
import com.example.movie_booking_app.viewmodel.LoginUiState
import com.example.movie_booking_app.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var fcmTokenHelper: FcmTokenHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.googleLogin(idToken)
            } else {
                showError("Google Sign-In failed: No ID token")
            }
        } catch (e: ApiException) {
            Log.w("LOGIN_DEBUG", "Google sign in failed", e)
            showError("Google Sign-In failed: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Hide error when user starts typing
        binding.edtUsername.addTextChangedListener { hideError() }
        binding.edtPassword.addTextChangedListener { hideError() }

        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            if (username.isEmpty()) {
                showError("Please enter username")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Please enter password")
                return@setOnClickListener
            }

            Log.d("LOGIN_DEBUG", "Username: $username")
            hideError()

            viewModel.login(LoginRequest(username, password))
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(com.example.movie_booking_app.BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        binding.btnLogin.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                    }
                    is LoginUiState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.pbLoading.visibility = View.VISIBLE
                    }
                    is LoginUiState.Success -> {
                        binding.btnLogin.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()

                        val username = binding.edtUsername.text.toString().trim()
                        val enteredOrDbUser = state.username

                        lifecycleScope.launch(Dispatchers.IO) {
                            fcmTokenHelper.registerCurrentToken("LoginActivity")
                        }
                        val roleName = state.role ?: ""

                        if (roleName.equals("Admin", ignoreCase = true)) {
                            val intent = Intent(this@LoginActivity, AdminActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else if (roleName.equals("Manager", ignoreCase = true) || roleName.equals("Cinema Manager", ignoreCase = true)) {
                            val intent = Intent(this@LoginActivity, ManagerActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        viewModel.resetState()
                    }
                    is LoginUiState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                        showError(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.txtError.text = message
        binding.txtError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.txtError.visibility = View.GONE
    }
}
