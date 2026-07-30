package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import timber.log.Timber
import com.example.movie_booking_app.utils.Validator
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : EnglishBaseActivity() {

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
            Timber.tag("LOGIN_DEBUG").w(e, "Google sign in failed")
            showError("Google Sign-In failed: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable system back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to disable back button
            }
        })

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

        //mentor note: validate in login at the same resigter
        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            val usernameError = Validator.validateUsername(username)
            if (usernameError != null) {
                showError(getString(usernameError))
                return@setOnClickListener
            }
            val passwordError = Validator.validatePassword(password)
            if (passwordError != null) {
                showError(getString(passwordError))
                return@setOnClickListener
            }
            // remove Log.d (used to run debug)
            // handle citizen identity card logic: 9 - 12 digits
            Timber.tag("LOGIN_DEBUG").d("Username: $username")
            hideError()

            viewModel.login(LoginRequest(username, password))
        }

        val googleClientId = com.example.movie_booking_app.BuildConfig.GOOGLE_CLIENT_ID
        if (googleClientId.isNotEmpty()) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleClientId)
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } else {
            Timber.tag("LOGIN_DEBUG").e("GOOGLE_CLIENT_ID is missing in local.properties or .env")
        }

        binding.btnGoogleLogin.setOnClickListener {
            if (::googleSignInClient.isInitialized) {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            } else {
                Toast.makeText(this, "Google Sign-In is not available", Toast.LENGTH_SHORT).show()
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

                        val loggedInUsername = state.username.ifEmpty {
                            binding.edtUsername.text.toString().trim()
                        }

                        if (loggedInUsername.isNotEmpty()) {
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("username", loggedInUsername)
                                .apply()

                            Timber.tag("LOGIN_DEBUG").d("Saved username to app_prefs: $loggedInUsername")
                        }
                        // use lifeCycle
                        lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
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
                        } else if (roleName.equals("Staff", ignoreCase = true) || roleName.equals("Employee", ignoreCase = true)) {
                            val intent = Intent(this@LoginActivity, StaffActivity::class.java)
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
