package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import timber.log.Timber
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityMainBinding
import com.example.movie_booking_app.view.fragment.GenericGridFragment
import com.example.movie_booking_app.view.fragment.MovieGridFragment
import com.example.movie_booking_app.view.fragment.HomeFragment
import com.example.movie_booking_app.view.fragment.PlaceholderFragment
import com.example.movie_booking_app.view.fragment.ProfileFragment
import com.example.movie_booking_app.view.fragment.MyTicketFragment
import com.example.movie_booking_app.viewmodel.AuthViewModel
import com.example.movie_booking_app.data.api.AuthEvent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.AndroidEntryPoint
import com.example.movie_booking_app.data.local.FcmTokenHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var fcmTokenHelper: FcmTokenHelper

    private lateinit var binding: ActivityMainBinding
    private var currentSection: String = ""
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Added by AI – feature auto login routing – 2026-07-06
        val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        val role = sharedPref.getString("role", "") ?: ""
        
        Timber.tag("MAIN_DEBUG").d("AutoLogin check - token: ${token?.take(10)}..., role: $role")
        
        if (!token.isNullOrEmpty()) {
            val destinationActivity = when {
                role.equals("Admin", ignoreCase = true) -> AdminActivity::class.java
                role.equals("Manager", ignoreCase = true) || role.equals("Cinema Manager", ignoreCase = true) -> ManagerActivity::class.java
                role.equals("Staff", ignoreCase = true) || role.equals("Employee", ignoreCase = true) -> StaffActivity::class.java
                else -> null
            }

            if (destinationActivity != null) {
                val intent = Intent(this, destinationActivity)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Preserve notification extras
                this.intent.extras?.let { intent.putExtras(it) }
                startActivity(intent)
                finish()
                return
            } else {
                // If it is a Member/User who auto-logins, register/refresh the FCM token on startup
                lifecycleScope.launch(Dispatchers.IO) {
                    fcmTokenHelper.registerCurrentToken("MainActivity")
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        setupBackStackListener()
        observeAuthEvents()

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = insets.bottom + (16 * resources.displayMetrics.density).toInt())
            windowInsets
        }
        
        // Handle explicit section requests from other activities or notifications
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Processes intent extras for deep-linking.
     * Handles both local navigation intents (e.g. TARGET_SECTION="Promo")
     * and Firebase push notification data payloads (e.g. type="booking", referenceId="xxx").
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) {
            navigateToSection("Home", force = true)
            return
        }

        // 1. Get requested target section, fallback to Home on standard launch
        val target = intent.getStringExtra("TARGET_SECTION") ?: "Home"

        // 2. Deep-linking from background/killed push notifications
        val type = intent.getStringExtra("type")
        val referenceId = intent.getStringExtra("referenceId")
        val title = intent.getStringExtra("title")
        val body = intent.getStringExtra("body")

        if (!type.isNullOrEmpty() || !referenceId.isNullOrEmpty() || !title.isNullOrEmpty()) {
            // First load the home/target section so the backstack has a valid parent
            navigateToSection(target, force = true)
            
            // Then navigate directly to NotificationActivity
            val destIntent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("type", type)
                putExtra("referenceId", referenceId)
                putExtra("title", title)
                putExtra("body", body)
            }
            startActivity(destIntent)
        } else {
            // Standard launch or local section request
            navigateToSection(target, force = true)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Added by AI for User – feature hide navbar – 2026-07-05
        val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            binding.bottomNav.visibility = android.view.View.GONE
        } else {
            binding.bottomNav.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.main_container)
            updateNavHighlightByFragment(fragment)
        }
    }

    private fun updateNavHighlightByFragment(fragment: Fragment?) {
        val target = when (fragment) {
            is HomeFragment -> "Home"
            is MovieGridFragment -> "Movie"
            is GenericGridFragment -> "Movie" 
            is PlaceholderFragment -> {
                val title = fragment.arguments?.getString("TITLE") ?: ""
                if (title.contains("Ticket", true)) "Ticket"
                else if (title.contains("Profile", true)) "Profile"
                else "Home"
            }
            is MyTicketFragment -> "Ticket"
            is ProfileFragment -> "Profile"
            else -> "Home"
        }
        highlightNav(target)
    }

    private fun setupBottomNav() {
        binding.btnNavHome.setOnClickListener { navigateToSection("Home") }
        binding.btnNavMovie.setOnClickListener { navigateToSection("Now Playing") }
        binding.btnNavTicket.setOnClickListener { navigateToSection("Ticket") }
        binding.btnNavProfile.setOnClickListener { navigateToSection("Profile") }
    }

    fun navigateToSection(section: String, force: Boolean = false) {
        if (!force && currentSection == section) return
        currentSection = section
        
        val fragment: Fragment = when (section) {
            "Home" -> HomeFragment()
            "Now Playing", "Coming Soon" -> MovieGridFragment.newInstance(section)
            "Ticket" -> MyTicketFragment()
            "Profile" -> ProfileFragment()
            "Promo", "Service", "News" -> GenericGridFragment.newInstance(section)
            else -> HomeFragment()
        }

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        
        if (section != "Home") {
            transaction.addToBackStack(section)
        } else {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
            
        transaction.commit()

        val navHighlight = if (section == "Now Playing" || section == "Coming Soon") "Movie" else section
        highlightNav(navHighlight)
    }

    private fun highlightNav(target: String) {
        val yellow = ContextCompat.getColor(this, R.color.yellow)
        val inactive = ContextCompat.getColor(this, R.color.nav_inactive)

        resetNavItem(binding.imgNavHome, binding.tvNavHome, inactive)
        resetNavItem(binding.imgNavMovie, binding.tvNavMovie, inactive)
        resetNavItem(binding.imgNavTicket, binding.tvNavTicket, inactive)
        resetNavItem(binding.imgNavProfile, binding.tvNavProfile, inactive)

        when(target) {
            "Home" -> highlightNavItem(binding.imgNavHome, binding.tvNavHome, yellow)
            "Movie", "Now Playing", "Coming Soon" -> highlightNavItem(binding.imgNavMovie, binding.tvNavMovie, yellow)
            "Ticket" -> highlightNavItem(binding.imgNavTicket, binding.tvNavTicket, yellow)
            "Profile" -> highlightNavItem(binding.imgNavProfile, binding.tvNavProfile, yellow)
        }
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            authViewModel.authEvents.collectLatest { event ->
                if (event is AuthEvent.SessionExpired) {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun resetNavItem(img: ImageView, tv: TextView, color: Int) {
        img.setColorFilter(color)
        tv.setTextColor(color)
    }

    private fun highlightNavItem(img: ImageView, tv: TextView, color: Int) {
        img.setColorFilter(color)
        tv.setTextColor(color)
    }
}
