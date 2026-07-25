package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.AuthEvent
import com.example.movie_booking_app.databinding.ActivityAdminBinding
import com.example.movie_booking_app.view.fragment.admin.AdminCinemasFragment
import com.example.movie_booking_app.view.fragment.admin.AdminMovieListFragment
import com.example.movie_booking_app.view.fragment.admin.AdminPromotionFragment
import com.example.movie_booking_app.view.fragment.admin.AdminStaffTabsFragment
import com.example.movie_booking_app.view.fragment.admin.AdminStatsFragment
import com.example.movie_booking_app.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeAuthEvents()

        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_movies -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_movies)
                    loadFragment(AdminMovieListFragment())
                }
                R.id.nav_dashboard -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_dashboard)
                    loadFragment(AdminStatsFragment())
                }
                R.id.nav_employees -> {
                    binding.tvHeaderTitle.text = getString(R.string.admin_staff_members_title)
                    loadFragment(AdminStaffTabsFragment.newInstance(0))
                }
                R.id.nav_rooms -> {
                    binding.tvHeaderTitle.text = "Cinemas"
                    loadFragment(AdminCinemasFragment())
                }
                R.id.nav_promotions -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_promotions)
                    loadFragment(AdminPromotionFragment())
                }
                R.id.nav_members -> {
                    binding.tvHeaderTitle.text = getString(R.string.admin_tab_members)
                    loadFragment(com.example.movie_booking_app.view.fragment.admin.MemberListFragment())
                }
                R.id.nav_settings -> {
                    binding.tvHeaderTitle.text = "Payrolls"
                    loadFragment(com.example.movie_booking_app.view.fragment.admin.AdminPayrollFragment())
                }
                R.id.nav_logout -> {
                    com.example.movie_booking_app.utils.AuthUtils.showLogoutConfirmationDialog(this)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set default selection to Dashboard (AdminStatsFragment)
        if (savedInstanceState == null) {
            binding.navigationView.setCheckedItem(R.id.nav_dashboard)
            binding.tvHeaderTitle.text = getString(R.string.auto_str_dashboard)
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
