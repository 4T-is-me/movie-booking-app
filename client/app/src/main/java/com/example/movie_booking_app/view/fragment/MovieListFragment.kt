package com.example.movie_booking_app.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.databinding.FragmentMovieListBinding
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.activity.LoginPlaceholderActivity
import com.example.movie_booking_app.view.adapter.HomeMultiAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import kotlinx.coroutines.launch

/**
 * MovieListFragment: Main dashboard utilizing View Binding and multi-viewType adapter.
 */
class MovieListFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MovieViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[MovieViewModel::class.java]
    }

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
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeMultiAdapter(
            onSearchQueryChanged = { query -> viewModel.searchMovies(query) },
            onSeeAllClicked = { sectionName ->
                (activity as? MainActivity)?.navigateToSection(sectionName)
            },
            onMovieClicked = { movie ->
                Toast.makeText(context, "Opening schedule for: ${movie.nameVn}", Toast.LENGTH_SHORT).show()
            },
            onLoginClicked = {
                startActivity(Intent(context, LoginPlaceholderActivity::class.java))
            }
        )
        binding.rvHome.adapter = homeAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeItems.collect { items ->
                if (items.isNotEmpty()) {
                    homeAdapter.submitList(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
