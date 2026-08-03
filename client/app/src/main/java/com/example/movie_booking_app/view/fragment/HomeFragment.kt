package com.example.movie_booking_app.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.NotificationLocalStorage
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.databinding.FragmentMovieListBinding
import com.example.movie_booking_app.view.activity.LoginActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.activity.MovieDetailActivity
import com.example.movie_booking_app.view.activity.NewsDetailActivity
import com.example.movie_booking_app.view.activity.NotificationActivity
import com.example.movie_booking_app.view.activity.VideoListActivity
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
        binding.layoutHeader.edtSearch.addTextChangedListener { text ->
            viewModel.searchMovies(text?.toString() ?: "")
        }

        binding.layoutHeader.btnLogin.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.layoutHeader.layoutNotificationBell.setOnClickListener {
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

        tvHi.text = context.getString(R.string.hi_user, name)

        if (isLoggedIn) {
            binding.layoutHeader.btnLogin.visibility = View.GONE
            binding.layoutHeader.layoutNotificationBell.visibility = View.VISIBLE
            
            // Check for unread notifications to show green badge dot
            val hasUnread = NotificationLocalStorage.getAll(context).any { !it.isRead }
            binding.layoutHeader.viewBellBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
        } else {
            binding.layoutHeader.btnLogin.visibility = View.VISIBLE
            binding.layoutHeader.layoutNotificationBell.visibility = View.GONE
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
            },
            onNewsClicked = { news ->
                val intent = Intent(context, NewsDetailActivity::class.java).apply {
                    putExtra("newsId", news.id)
                    putExtra("title", news.title)
                    putExtra("imageUrl", news.imageUrl)
                    putExtra("date", news.date)
                    putExtra("content", news.content ?: "")
                    putExtra("movieId", news.movieId)
                    putExtra("movieName", news.movieName)
                    putExtra("moviePoster", news.moviePoster)
                }
                startActivity(intent)
            },
            onNewsLikeClicked = { news ->
                viewModel.toggleNewsLike(news.id)
            }
        )
        binding.rvHome.adapter = homeAdapter
        (binding.rvHome.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeItems.collect { items ->
                // Filter out HeaderSection if it still exists in homeItems flow
                val listWithoutHeader = items.filter { it !is HomeItem.HeaderSection }
                homeAdapter.submitList(listWithoutHeader)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
