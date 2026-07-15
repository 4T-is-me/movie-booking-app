package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.fragment.admin.AdminMovieListFragment
import com.example.movie_booking_app.view.fragment.admin.AdminRoomListFragment
import com.example.movie_booking_app.view.fragment.admin.CinemaRoomsFragment
import com.example.movie_booking_app.view.fragment.admin.AdminStatsFragment
import com.example.movie_booking_app.view.fragment.admin.AdminStaffFragment
import com.example.movie_booking_app.view.fragment.admin.AdminStaffTabsFragment
import com.example.movie_booking_app.view.fragment.admin.AdminPromotionFragment
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.viewmodel.AuthViewModel
import com.example.movie_booking_app.data.api.AuthEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent

@AndroidEntryPoint
class AdminActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvHeaderTitle: TextView
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        observeAuthEvents()

        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_movies -> {
                    tvHeaderTitle.text = "Movies"
                    loadFragment(AdminMovieListFragment())
                }
                R.id.nav_dashboard -> {
                    tvHeaderTitle.text = "Dashboard"
                    loadFragment(AdminStatsFragment())
                }
                R.id.nav_employees -> {
                    tvHeaderTitle.text = "Staff & Members"
                    loadFragment(AdminStaffTabsFragment())
                }
                R.id.nav_rooms -> {
                    tvHeaderTitle.text = "Cinema Rooms"
                    loadFragment(CinemaRoomsFragment())
                }
                R.id.nav_promotions -> {
                    tvHeaderTitle.text = "Promotions"
                    loadFragment(AdminPromotionFragment())
                }
                R.id.nav_members -> {
                    tvHeaderTitle.text = "Staff & Members"
                    loadFragment(AdminStaffTabsFragment())
                }
                R.id.nav_settings -> {
                    tvHeaderTitle.text = "Settings"
                    loadFragment(BlankFragment())
                }
                R.id.nav_logout -> {
                    com.example.movie_booking_app.utils.AuthUtils.showLogoutConfirmationDialog(this)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set default selection to Dashboard (AdminStatsFragment)
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_dashboard)
            tvHeaderTitle.text = "Dashboard"
            loadFragment(AdminStatsFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            authViewModel.authEvents.collectLatest { event ->
                if (event is AuthEvent.SessionExpired) {
                    val intent = Intent(this@AdminActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }



    // A simple fragment that shows nothing
    class BlankFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return View(context)
        }
    }
}
