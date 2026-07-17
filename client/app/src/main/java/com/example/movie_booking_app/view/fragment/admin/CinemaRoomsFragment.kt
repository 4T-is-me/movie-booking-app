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

    companion object {
        private const val ARG_CINEMA_ID = "cinema_id"

        fun newInstance(cinemaId: String?): CinemaRoomsFragment {
            val fragment = CinemaRoomsFragment()
            val args = Bundle()
            args.putString(ARG_CINEMA_ID, cinemaId)
            fragment.arguments = args
            return fragment
        }
    }

    private var cinemaId: String? = null

    private val viewModel: CinemaRoomsViewModel by viewModels()
    private lateinit var adapter: CinemaRoomsAdapter

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvRooms: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var layoutEmptyState: View
    private lateinit var layoutErrorState: View
    private lateinit var tvTotalRooms: TextView
    private lateinit var tvTotalCinemaSeats: TextView
    private lateinit var tvRegularSeats: TextView
    private lateinit var tvVipSeats: TextView
    private lateinit var tvCoupleSeats: TextView
    private lateinit var btnAddRoom: MaterialButton
    private lateinit var btnEmptyAddRoom: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var btnBack: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cinemaId = arguments?.getString(ARG_CINEMA_ID)
    }

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
        tvTotalCinemaSeats = view.findViewById(R.id.tvTotalCinemaSeats)
        tvRegularSeats = view.findViewById(R.id.tvRegularSeats)
        tvVipSeats = view.findViewById(R.id.tvVipSeats)
        tvCoupleSeats = view.findViewById(R.id.tvCoupleSeats)
        btnAddRoom = view.findViewById(R.id.btnAddRoom)
        btnEmptyAddRoom = view.findViewById(R.id.btnEmptyAddRoom)
        btnRetry = view.findViewById(R.id.btnRetry)
        btnBack = view.findViewById(R.id.btnBack)

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
            viewModel.loadRooms(cinemaId)
        }

        // Add Room Button Click
        val onAddRoomClick = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddCinemaRoomFragment.newInstance(cinemaId))
                .addToBackStack(null)
                .commit()
        }
        btnAddRoom.setOnClickListener(onAddRoomClick)
        btnEmptyAddRoom.setOnClickListener(onAddRoomClick)

        // Retry Button Click
        btnRetry.setOnClickListener {
            viewModel.loadRooms(cinemaId)
        }

        // Back Button Click
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Observe ViewModel States
        observeViewModel()

        // Load data initially
        viewModel.loadRooms(cinemaId)
    }

    private fun observeViewModel() {
        // Observe filtered rooms (dynamic dataset used by RecyclerView)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredRooms.collectLatest { list ->
                adapter.submitList(list)
                // Set the summary card count dynamically based on the current dataset
                tvTotalRooms.text = if (list.size == 1) "1 room" else "${list.size} rooms"
                
                var total = 0
                var regular = 0
                var vip = 0
                var couple = 0
                
                for (room in list) {
                    total += room.totalSeats
                    regular += room.regularSeats
                    vip += room.vipSeats
                    couple += room.coupleSeats
                }
                
                tvTotalCinemaSeats.text = total.toString()
                tvRegularSeats.text = regular.toString()
                tvVipSeats.text = vip.toString()
                tvCoupleSeats.text = couple.toString()
                
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
