package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentCinemaRoomsBinding
import com.example.movie_booking_app.view.adapter.CinemaRoomsAdapter
import com.example.movie_booking_app.viewmodel.CinemaRoomsViewModel
import com.facebook.shimmer.ShimmerFrameLayout
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

    private var _binding: FragmentCinemaRoomsBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = FragmentCinemaRoomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views from binding
        swipeRefreshLayout = binding.swipeRefreshLayout
        rvRooms = binding.rvRooms
        shimmerContainer = binding.shimmerContainer
        layoutEmptyState = binding.layoutEmptyState
        layoutErrorState = binding.layoutErrorState
        tvTotalRooms = binding.tvTotalRooms
        tvTotalCinemaSeats = binding.tvTotalCinemaSeats
        tvRegularSeats = binding.tvRegularSeats
        tvVipSeats = binding.tvVipSeats
        tvCoupleSeats = binding.tvCoupleSeats
        btnAddRoom = binding.btnAddRoom
        btnEmptyAddRoom = binding.btnEmptyAddRoom
        btnRetry = binding.btnRetry
        btnBack = binding.btnBack

        // Set up RecyclerView
        adapter = CinemaRoomsAdapter(
            onRoomClick = { room ->
                // Seat layout viewing is disabled
            },
            onEditClick = { room ->
                // Navigate to AddCinemaRoomFragment in Edit Mode
                val fragment = AddCinemaRoomFragment.newInstance(cinemaId, room.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onToggleStatusClick = { room ->
                viewModel.toggleRoomStatus(room.id, room.status ?: "active", cinemaId)
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
                tvTotalRooms.text = if (list.size == 1) getString(R.string.one_room) else getString(R.string.rooms_format, list.size)
                
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
