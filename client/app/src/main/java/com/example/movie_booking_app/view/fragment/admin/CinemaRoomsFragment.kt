package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.CinemaRoomsAdapter
import com.example.movie_booking_app.viewmodel.CinemaRoomsViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CinemaRoomsFragment : Fragment() {

    private val viewModel: CinemaRoomsViewModel by viewModels()
    private lateinit var adapter: CinemaRoomsAdapter

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvRooms: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var layoutEmptyState: View
    private lateinit var layoutErrorState: View
    private lateinit var tvTotalRooms: TextView
    private lateinit var btnAddRoom: MaterialButton
    private lateinit var btnEmptyAddRoom: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cinema_rooms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        rvRooms = view.findViewById(R.id.rvRooms)
        shimmerContainer = view.findViewById(R.id.shimmerContainer)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        layoutErrorState = view.findViewById(R.id.layoutErrorState)
        tvTotalRooms = view.findViewById(R.id.tvTotalRooms)
        btnAddRoom = view.findViewById(R.id.btnAddRoom)
        btnEmptyAddRoom = view.findViewById(R.id.btnEmptyAddRoom)
        btnRetry = view.findViewById(R.id.btnRetry)
        searchView = view.findViewById(R.id.searchView)

        // Set up RecyclerView
        adapter = CinemaRoomsAdapter(
            onRoomClick = { room ->
                // Navigate to AdminRoomDetailFragment
                val fragment = AdminRoomDetailFragment.newInstance(
                    room.id,
                    room.roomName,
                    if (room.columns > 0) room.columns else 8
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        rvRooms.layoutManager = LinearLayoutManager(context)
        rvRooms.adapter = adapter

        // Set up pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadRooms()
        }

        // Set up search view listener
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

        // Add Room Button Click
        val onAddRoomClick = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCinemaRoomFragment())
                .addToBackStack(null)
                .commit()
        }
        btnAddRoom.setOnClickListener(onAddRoomClick)
        btnEmptyAddRoom.setOnClickListener(onAddRoomClick)

        // Retry Button Click
        btnRetry.setOnClickListener {
            viewModel.loadRooms()
        }

        // Observe ViewModel States
        observeViewModel()

        // Load data initially
        viewModel.loadRooms()
    }

    private fun observeViewModel() {
        // Observe filtered rooms (dynamic dataset used by RecyclerView)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredRooms.collectLatest { list ->
                adapter.submitList(list)
                // Set the summary card count dynamically based on the current dataset
                tvTotalRooms.text = if (list.size == 1) "1 room" else "${list.size} rooms"
                updateUiVisibility(list.isEmpty())
            }
        }

        // Observe Loading State
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

        // Observe Error State
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
}
