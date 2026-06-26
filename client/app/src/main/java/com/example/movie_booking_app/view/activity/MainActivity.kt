package com.example.movie_booking_app.view.activity

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentSection: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSafeAreas()
        setupBottomNav()
        setupBackStackListener()
        
        // Start with Home
        if (savedInstanceState == null) {
            navigateToSection("Home")
        }
    }

    private fun setupSafeAreas() {
        // Handle Safe Area for Main Container (Grid/Placeholder)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        // Handle Safe Area for System Navigation Bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }
    }

    private fun setupBackStackListener() {
        // Synchronize UI highlight when back stack changes
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
                // Determine based on current title or assume section
                if (currentSection == "Ticket") "Ticket"
                else if (currentSection == "Profile") "Profile"
                else "Home"
            }
            else -> "Home"
        }
        
        currentSection = target
        highlightNav(target)
    }

    private fun setupBottomNav() {
        binding.btnNavHome.setOnClickListener { navigateToSection("Home") }
        binding.btnNavMovie.setOnClickListener { navigateToSection("Now Playing") }
        binding.btnNavTicket.setOnClickListener { navigateToSection("Ticket") }
        binding.btnNavProfile.setOnClickListener { navigateToSection("Profile") }
    }

    fun navigateToSection(section: String) {
        if (currentSection == section) return
        currentSection = section
        
        val fragment: Fragment = when (section) {
            "Home" -> {
                highlightNav("Home")
                MovieListFragment()
            }
            "Now Playing", "Coming Soon" -> {
                highlightNav("Movie")
                MovieGridFragment.newInstance(section)
            }
            "Ticket" -> {
                highlightNav("Ticket")
                PlaceholderFragment.newInstance("Ticket System", R.drawable.ic_ticket)
            }
            "Profile" -> {
                highlightNav("Profile")
                PlaceholderFragment.newInstance("User Profile", R.drawable.ic_profile)
            }
            "Promo", "Service", "News" -> {
                highlightNav("None") 
                GenericGridFragment.newInstance(section)
            }
            else -> MovieListFragment()
        }

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        
        // Add to back stack if it's not the Home page
        if (section != "Home") {
            transaction.addToBackStack(section)
        } else {
            // Clear back stack when returning home to avoid infinite loops
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
            
        transaction.commit()
    }

    private fun highlightNav(target: String) {
        val yellow = ContextCompat.getColor(this, R.color.yellow)
        val inactive = ContextCompat.getColor(this, R.color.nav_inactive)

        // Reset all
        resetNavItem(binding.imgNavHome, binding.tvNavHome, inactive)
        resetNavItem(binding.imgNavMovie, binding.tvNavMovie, inactive)
        resetNavItem(binding.imgNavTicket, binding.tvNavTicket, inactive)
        resetNavItem(binding.imgNavProfile, binding.tvNavProfile, inactive)

        // Highlight target
        when(target) {
            "Home" -> highlightNavItem(binding.imgNavHome, binding.tvNavHome, yellow)
            "Movie" -> highlightNavItem(binding.imgNavMovie, binding.tvNavMovie, yellow)
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
