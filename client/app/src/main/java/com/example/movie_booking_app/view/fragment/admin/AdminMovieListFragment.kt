package com.example.movie_booking_app.view.fragment.admin

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
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.view.adapter.AdminMovieAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminMovieListFragment : Fragment() {

    private val viewModel: MovieViewModel by viewModels()
    private lateinit var adapter: AdminMovieAdapter

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
                val fragment = AdminCreateMovieFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("movie", movie)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { movie ->
                showDeleteConfirmationDialog(movie)
            }
        )

        rvAdminMovies.layoutManager = LinearLayoutManager(context)
        rvAdminMovies.adapter = adapter

        fabAddMovie.setOnClickListener {
            // TODO: Open Add Movie Fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdminCreateMovieFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel.loadAdminMovies()
        observeViewModel()
    }

    private fun showDeleteConfirmationDialog(movie: Movie) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Movie")
            .setMessage("Are you sure you want to delete the movie \"${movie.movieName}\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                movie.id?.let { viewModel.deleteMovie(it) }
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.adminMovies.collect { movies ->
                adapter.submitList(movies)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movieActionSuccess.collect { success ->
                val message = if (success) "Movie deleted successfully" else "Failed to delete movie"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movieActionMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}