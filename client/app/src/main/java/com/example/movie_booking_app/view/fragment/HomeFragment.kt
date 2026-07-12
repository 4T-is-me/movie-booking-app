package com.example.movie_booking_app.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentMovieListBinding
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.activity.LoginActivity
import com.example.movie_booking_app.view.activity.MovieDetailActivity
import com.example.movie_booking_app.view.activity.NotificationActivity
import com.example.movie_booking_app.view.activity.VideoListActivity
import com.example.movie_booking_app.data.local.NotificationLocalStorage
import com.example.movie_booking_app.view.adapter.HomeMultiAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MovieViewModel by viewModels()
    private lateinit var homeAdapter: HomeMultiAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        updateHeaderUI()
        // Added by AI – feature optimize performance – 2026-07-06
        // Removed viewModel.loadHomeData() here to prevent excessive API calls and UI freezing.
        // Data is already loaded in ViewModel init and cached in StateFlow.
    }

    private fun setupHeader() {
        val headerView = binding.layoutHeader.root
        val edtSearch = headerView.findViewById<EditText>(R.id.edtSearch)
        val btnLogin = headerView.findViewById<View>(R.id.btnLogin)
        val layoutNotificationBell = headerView.findViewById<View>(R.id.layoutNotificationBell)

        edtSearch.addTextChangedListener { text ->
            viewModel.searchMovies(text?.toString() ?: "")
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        layoutNotificationBell.setOnClickListener {
            startActivity(Intent(context, NotificationActivity::class.java))
        }

        updateHeaderUI()
    }

    private fun updateHeaderUI() {
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("username", "User") ?: "User"
        val isLoggedIn = !sharedPref.getString("auth_token", null).isNullOrEmpty()

        val headerView = binding.layoutHeader.root
        val tvHi = headerView.findViewById<TextView>(R.id.tvHi)
        val btnLogin = headerView.findViewById<View>(R.id.btnLogin)
        val layoutNotificationBell = headerView.findViewById<View>(R.id.layoutNotificationBell)
        val viewBellBadge = headerView.findViewById<View>(R.id.viewBellBadge)

        tvHi.text = "Hi, $name 👋"

        if (isLoggedIn) {
            btnLogin.visibility = View.GONE
            layoutNotificationBell.visibility = View.VISIBLE
            
            // Check for unread notifications to show green badge dot
            val hasUnread = NotificationLocalStorage.getAll(context).any { !it.isRead }
            viewBellBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
        } else {
            btnLogin.visibility = View.VISIBLE
            layoutNotificationBell.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeMultiAdapter(
            onSeeAllClicked = { sectionName ->
                if (sectionName == "Videos") {
                    val intent = Intent(context, VideoListActivity::class.java)
                    startActivity(intent)
                } else {
                    (activity as? MainActivity)?.navigateToSection(sectionName)
                }
            },
            onMovieClicked = { movie ->
                val intent = Intent(context, MovieDetailActivity::class.java)
                intent.putExtra("movieId", movie.id)
                startActivity(intent)
            }
        )
        binding.rvHome.adapter = homeAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeItems.collect { items ->
                // Filter out HeaderSection if it still exists in homeItems flow
                val listWithoutHeader = items.filter { it !is com.example.movie_booking_app.data.model.ui.HomeItem.HeaderSection }
                homeAdapter.submitList(listWithoutHeader)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
