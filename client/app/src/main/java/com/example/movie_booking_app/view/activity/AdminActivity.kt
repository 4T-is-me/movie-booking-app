package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.movie_booking_app.R
import com.google.android.material.navigation.NavigationView

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
                R.id.nav_dashboard -> {
                    tvHeaderTitle.text = "Dashboard"
                    // Load Dashboard Fragment
                }
                R.id.nav_movies -> {
                    tvHeaderTitle.text = "Create Movie"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AdminCreateMovieFragment())
                        .commit()
                }
                R.id.nav_employees -> {
                    tvHeaderTitle.text = "Employees"
                }
                R.id.nav_rooms -> {
                    tvHeaderTitle.text = "Cinema Rooms"
                }
                R.id.nav_promotions -> {
                    tvHeaderTitle.text = "Promotions"
                }
                R.id.nav_members -> {
                    tvHeaderTitle.text = "Members"
                }
                R.id.nav_settings -> {
                    tvHeaderTitle.text = "Settings"
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set default selection
        navigationView.setCheckedItem(R.id.nav_dashboard)
    }
}
