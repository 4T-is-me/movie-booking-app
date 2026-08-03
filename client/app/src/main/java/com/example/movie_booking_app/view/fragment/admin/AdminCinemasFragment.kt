package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentAdminCinemasBinding
import com.example.movie_booking_app.view.adapter.CinemasAdapter
import com.example.movie_booking_app.viewmodel.AdminCinemasViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminCinemasFragment : Fragment() {

    private val viewModel: AdminCinemasViewModel by viewModels()
    private lateinit var adapter: CinemasAdapter

    private var _binding: FragmentAdminCinemasBinding? = null
    private val binding get() = _binding!!

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCinemas: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var layoutEmptyState: View
    private lateinit var layoutErrorState: View
    private lateinit var tvTotalCinemas: TextView
    private lateinit var btnAddCinema: MaterialButton
    private lateinit var btnEmptyAddCinema: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminCinemasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout = binding.swipeRefreshLayout
        rvCinemas = binding.rvCinemas
        shimmerContainer = binding.shimmerContainer
        layoutEmptyState = binding.layoutEmptyState
        layoutErrorState = binding.layoutErrorState
        tvTotalCinemas = binding.tvTotalCinemas
        btnAddCinema = binding.btnAddCinema
        btnEmptyAddCinema = binding.btnEmptyAddCinema
        btnRetry = binding.btnRetry
        searchView = binding.searchView

        adapter = CinemasAdapter(
            onCinemaClick = { cinema ->
                val fragment = CinemaRoomsFragment.newInstance(cinema.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onEditClick = { cinema ->
                val fragment = AddCinemaFragment.newInstance(cinema.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onToggleStatusClick = { cinema ->
                viewModel.toggleCinemaStatus(cinema.id ?: "", cinema.status ?: "active")
            }
        )
        rvCinemas.layoutManager = LinearLayoutManager(context)
        rvCinemas.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCinemas()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })

        val onAddCinemaClick = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCinemaFragment())
                .addToBackStack(null)
                .commit()
        }
        btnAddCinema.setOnClickListener(onAddCinemaClick)
        btnEmptyAddCinema.setOnClickListener(onAddCinemaClick)

        btnRetry.setOnClickListener {
            viewModel.loadCinemas()
        }

        observeViewModel()
        viewModel.loadCinemas()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredCinemas.collectLatest { list ->
                adapter.submitList(list)
                tvTotalCinemas.text = if (list.size == 1) "1 cinema" else "${list.size} cinemas"
                updateUiVisibility(list.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    if (!swipeRefreshLayout.isRefreshing) {
                        shimmerContainer.visibility = View.VISIBLE
                        shimmerContainer.startShimmer()
                        swipeRefreshLayout.visibility = View.GONE
                    }
                    layoutEmptyState.visibility = View.GONE
                    layoutErrorState.visibility = View.GONE
                } else {
                    shimmerContainer.stopShimmer()
                    shimmerContainer.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    swipeRefreshLayout.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMsg ->
                if (errorMsg != null) {
                    layoutErrorState.visibility = View.VISIBLE
                    swipeRefreshLayout.visibility = View.GONE
                    layoutEmptyState.visibility = View.GONE
                } else {
                    layoutErrorState.visibility = View.GONE
                }
            }
        }
    }

    private fun updateUiVisibility(isListEmpty: Boolean) {
        val isLoading = viewModel.isLoading.value
        val hasError = viewModel.error.value != null

        if (isLoading || hasError) return

        if (isListEmpty) {
            layoutEmptyState.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCinemas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
