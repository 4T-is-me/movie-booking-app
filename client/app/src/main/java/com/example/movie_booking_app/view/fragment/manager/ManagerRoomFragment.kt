package com.example.movie_booking_app.view.fragment.manager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.FragmentManagerRoomListBinding
import com.example.movie_booking_app.view.activity.manager.ManagerSeatActivity
import com.example.movie_booking_app.view.adapter.manager.ManagerRoomAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerRoomFragment : Fragment() {

    private var _binding: FragmentManagerRoomListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var adapter: ManagerRoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerRoomListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the local header since ManagerActivity has its own header
        binding.ivBack.parent?.let {
            if (it is View) it.visibility = View.GONE
        }

        adapter = ManagerRoomAdapter(
            onRoomClick = { room ->
                val intent = Intent(requireContext(), ManagerSeatActivity::class.java)
                intent.putExtra("roomId", room.id)
                intent.putExtra("roomName", room.roomName)
                intent.putExtra("roomType", room.roomType)
                intent.putExtra("status", room.status)
                intent.putExtra("totalSeats", room.totalSeats)
                intent.putExtra("regularSeats", room.regularSeats)
                intent.putExtra("vipSeats", room.vipSeats)
                intent.putExtra("coupleSeats", room.coupleSeats)
                intent.putExtra("regularPrice", room.regularPrice)
                intent.putExtra("vipPrice", room.vipPrice)
                intent.putExtra("couplePrice", room.couplePrice)
                intent.putExtra("estFullCapacity", room.estFullCapacity)
                startActivity(intent)
            }
        )

        binding.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRooms.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rooms.collect { rooms ->
                adapter.submitList(rooms)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadRooms()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRooms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
