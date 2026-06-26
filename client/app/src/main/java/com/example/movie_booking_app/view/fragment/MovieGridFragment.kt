package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.RepositoryProvider
import com.example.movie_booking_app.databinding.LayoutMovieGridContentBinding
import com.example.movie_booking_app.view.adapter.MovieGridAdapter
import kotlinx.coroutines.launch

class MovieGridFragment : Fragment() {

    private var _binding: LayoutMovieGridContentBinding? = null
    private val binding get() = _binding!!

    private val movieRepository: MovieRepository = RepositoryProvider.provideMovieRepository()
    private var currentSection: String = "Now Playing"

    companion object {
        fun newInstance(sectionName: String): MovieGridFragment {
            val fragment = MovieGridFragment()
            val args = Bundle()
            args.putString("SECTION_NAME", sectionName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutMovieGridContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentSection = arguments?.getString("SECTION_NAME") ?: "Now Playing"
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupTabs()
        setupRecyclerView()
        switchTab(currentSection)
    }

    private fun setupTabs() {
        binding.tabNowPlaying.setOnClickListener { switchTab("Now Playing") }
        binding.tabComingSoon.setOnClickListener { switchTab("Coming Soon") }
    }

    private fun switchTab(section: String) {
        currentSection = section
        val isNowPlaying = (section == "Now Playing")
        updateTabStyle(binding.tabNowPlaying, isNowPlaying)
        updateTabStyle(binding.tabComingSoon, !isNowPlaying)
        loadMovieData(isNowPlaying)
    }

    private fun updateTabStyle(tab: TextView, isActive: Boolean) {
        context?.let { ctx ->
            if (isActive) {
                tab.setBackgroundResource(R.drawable.bg_tab_active)
                tab.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            } else {
                tab.background = null
                tab.setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvFullList.layoutManager = GridLayoutManager(context, 2)
    }

    private fun loadMovieData(isNowPlaying: Boolean) {
        val adapter = MovieGridAdapter(isNowPlaying) { movie ->
            Toast.makeText(context, "Opening schedule for: ${movie.movieName}", Toast.LENGTH_SHORT).show()
        }
        binding.rvFullList.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            movieRepository.getMovies().collect { allMovies ->
                val filtered = allMovies.filter { 
                    if (isNowPlaying) it.status == "showing" else it.status == "coming_soon"
                }
                adapter.submitList(filtered)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
