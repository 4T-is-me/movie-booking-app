package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.AdminMovieAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.RepositoryProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminMovieListFragment : Fragment() {

    private val viewModel: MovieViewModel by viewModels()
    private lateinit var adapter: AdminMovieAdapter
    private val movieRepository: MovieRepository = RepositoryProvider.provideMovieRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_movie_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvAdminMovies = view.findViewById<RecyclerView>(R.id.rvAdminMovies)
        val fabAddMovie = view.findViewById<FloatingActionButton>(R.id.fabAddMovie)

        adapter = AdminMovieAdapter(
            onEditClick = { movie ->
                Toast.makeText(context, "Edit: ${movie.movieName}", Toast.LENGTH_SHORT).show()
                // TODO: Open Edit Fragment
            },
            onDeleteClick = { movie ->
                movie.id?.let { viewModel.deleteMovie(it) }
            }
        )

        rvAdminMovies.layoutManager = LinearLayoutManager(context)
        rvAdminMovies.adapter = adapter

        fabAddMovie.setOnClickListener {
            // TODO: Open Add Movie Fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdminAddMovieFragment())
                .addToBackStack(null)
                .commit()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            movieRepository.getMovies().collect { movies ->
                adapter.submitList(movies)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movieActionSuccess.collect { success ->
                val message = if (success) "Action successful!" else "Action failed!"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
