package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentAdminRoomDetailBinding
import com.example.movie_booking_app.view.adapter.AdminSeatAdapter
import com.example.movie_booking_app.viewmodel.AdminRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminRoomDetailFragment : Fragment() {

    private val viewModel: AdminRoomViewModel by viewModels()
    private lateinit var adapter: AdminSeatAdapter
    private var roomId: String? = null
    private var roomName: String? = null
    private var columns: Int = 8

    private var _binding: FragmentAdminRoomDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(roomId: String, roomName: String, columns: Int): AdminRoomDetailFragment {
            val fragment = AdminRoomDetailFragment()
            val args = Bundle()
            args.putString("ROOM_ID", roomId)
            args.putString("ROOM_NAME", roomName)
            args.putInt("COLUMNS", columns)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = arguments?.getString("ROOM_ID")
        roomName = arguments?.getString("ROOM_NAME")
        columns = arguments?.getInt("COLUMNS") ?: 8
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminRoomDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRoomTitle.text = roomName ?: getString(R.string.seat_layout)
        binding.tvRoomSummary.text = getString(R.string.auto_str_tap_on_seat_to_change_type)

        adapter = AdminSeatAdapter(
            onSeatClick = { seat ->
                viewModel.toggleSeatType(seat)
            }
        )

        binding.rvSeatGrid.layoutManager = GridLayoutManager(context, columns)
        binding.rvSeatGrid.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seats.collect { seats ->
                adapter.submitList(seats)
                binding.tvRoomSummary.text = getString(R.string.seat_layout_format, seats.size)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        roomId?.let { viewModel.loadRoomSeats(it) }

        binding.btnSaveLayout.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}