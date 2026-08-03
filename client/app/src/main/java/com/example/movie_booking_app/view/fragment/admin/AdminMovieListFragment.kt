package com.example.movie_booking_app.view.fragment.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.databinding.DialogConfirmDeleteStaffBinding
import com.example.movie_booking_app.databinding.FragmentAdminMovieListBinding
import com.example.movie_booking_app.view.adapter.AdminMovieAdapter
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.viewmodel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
                        putSerializable(Constants.Movie.EXTRA_MOVIE_DATA, movie)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { movie ->
                showDeleteConfirmationDialog(movie)
            },
            onRestoreClick = { movie ->
                showRestoreConfirmationDialog(movie)
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

        dialogBinding.tvDeleteTitle.text = getString(R.string.auto_str_delete_movie_title)
        dialogBinding.tvDeleteMessage.text = getString(R.string.auto_str_delete_confirm_msg, movie.movieName)

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            dialog.dismiss()
            val currentStatus = if (selectedTab == TAB_ACTIVE) Constants.Movie.STATUS_ACTIVE else Constants.Movie.STATUS_DELETED
            movie.id?.let { viewModel.deleteMovie(it, currentStatus) }
        }

        dialog.show()
    }

    private fun showRestoreConfirmationDialog(movie: Movie) {
        val dialogBinding = DialogConfirmDeleteStaffBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        // Customize the dialog for restoration
        dialogBinding.tvDeleteTitle.text = getString(R.string.auto_str_restore_movie_title)
        dialogBinding.tvDeleteMessage.text = getString(R.string.auto_str_restore_confirm_msg, movie.movieName)
        dialogBinding.ivDeleteIcon.setImageResource(android.R.drawable.ic_menu_rotate)
        dialogBinding.btnConfirmDelete.text = getString(R.string.auto_str_confirm_dialog)
        
        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            dialog.dismiss()
            val request = UpdateMovieRequest(
                isDeleted = false
            )
            val currentStatus = if (selectedTab == TAB_ACTIVE) Constants.Movie.STATUS_ACTIVE else Constants.Movie.STATUS_DELETED
            movie.id?.let { viewModel.updateMovie(it, request, null, currentStatus) }
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
                val message = if (success) {
                    if (selectedTab == TAB_ACTIVE) "Movie deleted successfully" else "Movie restored successfully"
                } else {
                    if (selectedTab == TAB_ACTIVE) "Failed to delete movie" else "Failed to restore movie"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movieActionMessage.collect { message ->
                val displayMsg = when(message) {
                    "msg_movie_deleted_success" -> getString(R.string.msg_movie_deleted_success)
                    "msg_movie_restored_success" -> getString(R.string.msg_movie_restored_success)
                    "msg_movie_delete_failed" -> getString(R.string.msg_movie_delete_failed)
                    "msg_movie_restore_failed" -> getString(R.string.msg_movie_restore_failed)
                    "msg_movie_added_success" -> getString(R.string.msg_movie_added_success)
                    "msg_movie_add_failed" -> getString(R.string.msg_movie_add_failed)
                    "msg_movie_updated_success" -> getString(R.string.msg_movie_updated_success)
                    "msg_movie_update_failed" -> getString(R.string.msg_movie_update_failed)
                    "error_unknown" -> getString(R.string.error_unknown)
                    else -> message
                }
                Toast.makeText(context, displayMsg, Toast.LENGTH_SHORT).show()
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
