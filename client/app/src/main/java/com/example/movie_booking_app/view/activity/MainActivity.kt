package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.databinding.ActivityMainBinding
import com.example.movie_booking_app.view.adapter.ComingSoonAdapter
import com.example.movie_booking_app.view.adapter.NowPlayingAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Login Button
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Bottom Navigation
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_ticket -> {
                    Toast.makeText(this, "Ticket Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_movie -> {
                    Toast.makeText(this, "Movie Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }

        val rvNowPlaying = binding.rvNowPlaying
        val rvComingSoon = binding.rvComingSoon
        
        val snapHelper = androidx.recyclerview.widget.PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvNowPlaying)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.movieApi.getMovies(100)
                if (response.isSuccessful) {
                    val movies = response.body()?.data ?: emptyList()
                    
                    // 7 highest rated movies for showing
                    val nowPlayingMovies = movies.filter { it.status == "showing" }
                        .sortedByDescending { it.rating ?: 0.0 }
                        .take(7)
                    
                    // 7 random movies for coming soon
                    val comingSoonMovies = movies.filter { it.status == "coming-soon" || it.status == "coming_soon" }
                        .shuffled()
                        .take(7)
                    
                    withContext(Dispatchers.Main) {
                        rvNowPlaying.adapter = NowPlayingAdapter(nowPlayingMovies)
                        rvComingSoon.adapter = ComingSoonAdapter(comingSoonMovies)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to load movies", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://res.cloudinary.com/dhc4obnog/image/upload/v1782362361/icon_rmgstg.png")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
                val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                withContext(Dispatchers.Main) {
                    binding.ivWelcomeLogo.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
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
            binding.btnLogin.visibility = android.view.View.GONE
            binding.tvHi.visibility = android.view.View.VISIBLE
            binding.tvWelcomeBack.visibility = android.view.View.VISIBLE
            binding.cvWelcomeLogo.visibility = android.view.View.GONE
            binding.tvHi.text = "Hi, $username \uD83D\uDC4B"
        } else {
            binding.btnLogin.visibility = android.view.View.VISIBLE
            binding.tvHi.visibility = android.view.View.INVISIBLE
            binding.tvWelcomeBack.visibility = android.view.View.INVISIBLE
            binding.cvWelcomeLogo.visibility = android.view.View.VISIBLE
        }
    }
}