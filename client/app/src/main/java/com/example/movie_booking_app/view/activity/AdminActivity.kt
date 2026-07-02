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
import com.example.movie_booking_app.view.fragment.admin.AdminStatsFragment
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvHeaderTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

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
                    tvHeaderTitle.text = "Employees"
                    loadFragment(BlankFragment())
                }
                R.id.nav_rooms -> {
                    tvHeaderTitle.text = "Cinema Rooms"
                    loadFragment(AdminRoomListFragment())
                }
                R.id.nav_promotions -> {
                    tvHeaderTitle.text = "Promotions"
                    loadFragment(BlankFragment())
                }
                R.id.nav_members -> {
                    tvHeaderTitle.text = "Members"
                    loadFragment(BlankFragment())
                }
                R.id.nav_settings -> {
                    tvHeaderTitle.text = "Settings"
                    loadFragment(BlankFragment())
                }
                R.id.nav_logout -> {
                    com.example.movie_booking_app.data.local.TokenManager.clearTokens(this@AdminActivity)
                    val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        remove("auth_token")
                        remove("username")
                        remove("role")
                        apply()
                    }
                    val intent = android.content.Intent(this@AdminActivity, com.example.movie_booking_app.view.activity.LoginActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
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
