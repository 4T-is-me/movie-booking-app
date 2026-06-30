package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.AdminRoomAdapter
import com.example.movie_booking_app.viewmodel.AdminRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminRoomListFragment : Fragment() {

    private val viewModel: AdminRoomViewModel by viewModels()
    private lateinit var adapter: AdminRoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_room_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvRooms = view.findViewById<RecyclerView>(R.id.rvRooms)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvTotalRoomsCount = view.findViewById<android.widget.TextView>(R.id.tvTotalRoomsCount)

        adapter = AdminRoomAdapter(
            onRoomClick = { room ->
                val fragment = AdminRoomDetailFragment.newInstance(room.id, room.roomName, room.columns)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        rvRooms.layoutManager = LinearLayoutManager(context)
        rvRooms.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rooms.collect { rooms ->
                adapter.submitList(rooms)
                tvTotalRoomsCount.text = "${rooms.size} phòng"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadRooms()
    }
}
