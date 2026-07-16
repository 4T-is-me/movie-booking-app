package com.example.movie_booking_app.view.activity.manager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.manager.ManagerRoomAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerRoomListActivity : AppCompatActivity() {

    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var adapter: ManagerRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_room_list)

        val rvRooms = findViewById<RecyclerView>(R.id.rvRooms)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        ivBack.setOnClickListener { finish() }

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

        rvRooms.layoutManager = LinearLayoutManager(this)
        rvRooms.adapter = adapter

        lifecycleScope.launch {
            viewModel.rooms.collect { rooms ->
                adapter.submitList(rooms)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadRooms()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRooms()
    }
}
