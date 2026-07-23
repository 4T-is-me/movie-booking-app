package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.databinding.FragmentAdminMovieListBinding
import com.example.movie_booking_app.databinding.DialogConfirmDeleteStaffBinding
import com.example.movie_booking_app.view.adapter.AdminMovieAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.widget.TextView

@AndroidEntryPoint
class AdminMovieListFragment : Fragment() {

    private val viewModel: MovieViewModel by activityViewModels()
    private lateinit var adapter: AdminMovieAdapter
    private var allMovies: List<Movie> = emptyList()
    private var selectedTab = TAB_ACTIVE

    private var _binding: FragmentAdminMovieListBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAB_ACTIVE = "active"
        private const val TAB_INACTIVE = "inactive"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.rvAdminMovies.layoutManager = LinearLayoutManager(context)
        binding.rvAdminMovies.adapter = adapter

        binding.etSearchMovie.addTextChangedListener { text ->
            adapter.submitList(getFilteredMovies(allMovies, text?.toString().orEmpty()))
        }

        binding.tabActiveMovie.setOnClickListener {
            switchTab(TAB_ACTIVE)
            viewModel.loadAdminMovies("active")
        }
        binding.tabInactiveMovie.setOnClickListener {
            switchTab(TAB_INACTIVE)
            viewModel.loadAdminMovies("deleted")
        }

        binding.fabAddMovie.setOnClickListener {
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
        val dialogBinding = DialogConfirmDeleteStaffBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        dialogBinding.tvDeleteMessage.text = "Are you sure you want to delete the movie \"${movie.movieName}\"?"

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            dialog.dismiss()
            movie.id?.let { viewModel.deleteMovie(it) }
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.adminMovies.collect { movies ->
                allMovies = movies
                refreshMovieList()
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

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab
        refreshMovieList()
    }

    private fun refreshMovieList() {
        val isActive = selectedTab == TAB_ACTIVE
        updateTabStyle(binding.tabActiveMovie, isActive)
        updateTabStyle(binding.tabInactiveMovie, !isActive)
        adapter.submitList(getFilteredMovies(allMovies, binding.etSearchMovie.text.toString().trim()))
    }

    private fun updateTabStyle(tabView: TextView, isActive: Boolean) {
        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_tab_active)
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
        }
    }

    private fun getFilteredMovies(fullList: List<Movie>, searchQuery: String): List<Movie> {
        val query = searchQuery.trim().lowercase()
        return fullList.filter { movie ->
            val matchesSearch = query.isEmpty() || movie.movieName?.lowercase()?.contains(query) == true
            val matchesTab = if (selectedTab == TAB_ACTIVE) {
                movie.isDeleted != true
            } else {
                movie.isDeleted == true
            }
            matchesSearch && matchesTab
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}