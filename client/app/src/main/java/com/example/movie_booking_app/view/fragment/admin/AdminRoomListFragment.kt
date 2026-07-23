package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentAdminRoomListBinding
import com.example.movie_booking_app.view.adapter.AdminRoomAdapter
import com.example.movie_booking_app.viewmodel.AdminRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminRoomListFragment : Fragment() {

    private val viewModel: AdminRoomViewModel by viewModels()
    private lateinit var adapter: AdminRoomAdapter

    private var _binding: FragmentAdminRoomListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminRoomListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminRoomAdapter(
            onRoomClick = { room ->
                val fragment =
                    AdminRoomDetailFragment.newInstance(room.id, room.roomName, room.columns)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        binding.rvRooms.layoutManager = LinearLayoutManager(context)
        binding.rvRooms.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rooms.collect { rooms ->
                adapter.submitList(rooms)
                binding.tvTotalRoomsCount.text = "${rooms.size} rooms"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadRooms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}