package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.movie_booking_app.view.fragment.MovieListFragment
import com.example.movie_booking_app.view.fragment.PlaceholderFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentSection: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNavHome.setOnClickListener { navigateToSection("Home") }
        binding.btnNavMovie.setOnClickListener { navigateToSection("Movie") }
        binding.btnNavTicket.setOnClickListener { navigateToSection("Ticket") }
        binding.btnNavProfile.setOnClickListener { navigateToSection("Profile") }
        
        // Handle explicit section requests from other activities (like Profile)
        val target = intent.getStringExtra("TARGET_SECTION") ?: "Home"
        navigateToSection(target, force = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("TARGET_SECTION")?.let { target ->
            navigateToSection(target, force = true)
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        val username = sharedPref.getString("username", "User")
        val role = sharedPref.getString("role", "user")
        if (token != null) {
            if (role?.contains("admin", ignoreCase = true) == true || username?.contains("admin", ignoreCase = true) == true) {
                val intent = Intent(this, AdminActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }
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
            is MovieListFragment -> "Home"
            is MovieGridFragment -> "Movie"
            is GenericGridFragment -> "Movie" 
            is PlaceholderFragment -> {
                val title = fragment.arguments?.getString("TITLE") ?: ""
                if (title.contains("Ticket", true)) "Ticket"
                else if (title.contains("Profile", true)) "Profile"
                else "Home"
            }
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
            "Home" -> MovieListFragment()
            "Now Playing", "Coming Soon" -> MovieGridFragment.newInstance(section)
            "Ticket" -> PlaceholderFragment.newInstance("Ticket System", R.drawable.ic_ticket)
            "Profile" -> {
                highlightNav("Profile")
                startActivity(Intent(this, ProfileActivity::class.java))
                return
            }
            "Promo", "Service", "News" -> GenericGridFragment.newInstance(section)
            else -> MovieListFragment()
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

    private fun resetNavItem(img: ImageView, tv: TextView, color: Int) {
        img.setColorFilter(color)
        tv.setTextColor(color)
    }

    private fun highlightNavItem(img: ImageView, tv: TextView, color: Int) {
        img.setColorFilter(color)
        tv.setTextColor(color)
    }
}
