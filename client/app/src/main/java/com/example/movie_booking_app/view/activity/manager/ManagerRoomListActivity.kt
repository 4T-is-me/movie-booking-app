package com.example.movie_booking_app.view.activity.manager

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivityManagerRoomListBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerRoomAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerRoomListActivity : AppCompatActivity() {

    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var binding: ActivityManagerRoomListBinding
    private lateinit var adapter: ManagerRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerRoomListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        adapter = ManagerRoomAdapter(
            onRoomClick = { room ->
                val intent = Intent(this, ManagerSeatActivity::class.java)
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

        binding.rvRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRooms.adapter = adapter

        lifecycleScope.launch {
            viewModel.rooms.collect { rooms ->
                adapter.submitList(rooms)
            }
        }

        lifecycleScope.launch {
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
}
