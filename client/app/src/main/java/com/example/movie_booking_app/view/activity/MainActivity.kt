package com.example.movie_booking_app.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.AuthEvent
import com.example.movie_booking_app.data.local.FcmTokenHelper
import com.example.movie_booking_app.databinding.ActivityMainBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.RoleConstants
import com.example.movie_booking_app.view.fragment.GenericGridFragment
import com.example.movie_booking_app.view.fragment.HomeFragment
import com.example.movie_booking_app.view.fragment.MovieGridFragment
import com.example.movie_booking_app.view.fragment.MyTicketFragment
import com.example.movie_booking_app.view.fragment.PlaceholderFragment
import com.example.movie_booking_app.view.fragment.ProfileFragment
import com.example.movie_booking_app.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

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
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        val role = sharedPref.getString("role", "") ?: ""
        
        Timber.tag("MAIN_DEBUG").d("AutoLogin check - token: ${token?.take(10)}..., role: $role")
        
        if (!token.isNullOrEmpty()) {
            val destinationActivity = when {
                role.equals(RoleConstants.ADMIN, ignoreCase = true) -> AdminActivity::class.java
                role.equals(RoleConstants.MANAGER, ignoreCase = true) || role.equals(RoleConstants.CINEMA_MANAGER, ignoreCase = true) -> ManagerActivity::class.java
                role.equals(RoleConstants.STAFF, ignoreCase = true) || role.equals(RoleConstants.EMPLOYEE, ignoreCase = true) -> StaffActivity::class.java
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
        val target = intent.getStringExtra("TARGET_SECTION") ?: Constants.NAV.HOME

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
        
        // Added by AI for User – feature hide navbar & AI chat FAB for logged-in users only – 2026-07-26
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            binding.bottomNav.visibility = View.GONE
            binding.fabAiChat.visibility = View.GONE
        } else {
            binding.bottomNav.visibility = View.VISIBLE
            binding.fabAiChat.visibility = View.VISIBLE
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
            is HomeFragment -> Constants.NAV.HOME
            is MovieGridFragment -> Constants.NAV.MOVIE
            is GenericGridFragment -> Constants.NAV.MOVIE
            is PlaceholderFragment -> {
                val title = fragment.arguments?.getString("TITLE") ?: ""
                if (title.contains(Constants.NAV.TICKET, true)) Constants.NAV.TICKET
                else if (title.contains(Constants.NAV.PROFILE, true)) Constants.NAV.PROFILE
                else Constants.NAV.HOME
            }
            is MyTicketFragment -> Constants.NAV.TICKET
            is ProfileFragment -> Constants.NAV.PROFILE
            else ->  Constants.NAV.HOME
        }
        highlightNav(target)
    }

    private fun setupBottomNav() {
        binding.btnNavHome.setOnClickListener { navigateToSection(Constants.NAV.HOME) }
        binding.btnNavMovie.setOnClickListener { navigateToSection(Constants.NAV.NOW_PLAYING) }
        binding.btnNavTicket.setOnClickListener { navigateToSection(Constants.NAV.TICKET) }
        binding.btnNavProfile.setOnClickListener { navigateToSection(Constants.NAV.PROFILE) }
        binding.fabAiChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
    }

    fun navigateToSection(section: String, force: Boolean = false) {
        if (!force && currentSection == section) return
        currentSection = section
        
        val fragment: Fragment = when (section) {
            Constants.NAV.HOME -> HomeFragment()
            Constants.NAV.NOW_PLAYING, Constants.NAV.COMING_SOON -> MovieGridFragment.newInstance(section)
            Constants.NAV.TICKET -> MyTicketFragment()
            Constants.NAV.PROFILE -> ProfileFragment()
            Constants.SECTION_NAMES.PROMO, Constants.SECTION_NAMES.SERVICE,Constants.SECTION_NAMES.NEWS -> GenericGridFragment.newInstance(section)
            else -> HomeFragment()
        }

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        
        if (section != Constants.NAV.HOME) {
            transaction.addToBackStack(section)
        } else {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
            
        transaction.commit()

        val navHighlight = if (section == Constants.NAV.NOW_PLAYING || section == Constants.NAV.COMING_SOON) Constants.NAV.MOVIE else section
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
            Constants.NAV.HOME -> highlightNavItem(binding.imgNavHome, binding.tvNavHome, yellow)
            Constants.NAV.MOVIE, Constants.NAV.NOW_PLAYING, Constants.NAV.COMING_SOON -> highlightNavItem(binding.imgNavMovie, binding.tvNavMovie, yellow)
            Constants.NAV.TICKET -> highlightNavItem(binding.imgNavTicket, binding.tvNavTicket, yellow)
            Constants.NAV.PROFILE -> highlightNavItem(binding.imgNavProfile, binding.tvNavProfile, yellow)
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
