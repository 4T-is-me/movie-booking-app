package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemAdminRoomBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.CinemaRoom

class AdminRoomAdapter(
    private val onRoomClick: (CinemaRoom) -> Unit
) : ListAdapter<CinemaRoom, AdminRoomAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemAdminRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room)
        holder.itemView.setOnClickListener { onRoomClick(room) }
    }

    class RoomViewHolder(private val binding: ItemAdminRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvRoomName: TextView = binding.tvRoomName
        private val tvTotalSeats: TextView = binding.tvTotalSeats
        private val tvRoomHint: TextView = binding.tvRoomHint

        fun bind(room: CinemaRoom) {
            tvRoomName.text = room.roomName
            tvTotalSeats.text = "Total Seats: ${room.totalSeats}"
            tvRoomHint.text = getString(R.string.auto_str_tap_to_edit_seat_layout)
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<CinemaRoom>() {
        override fun areItemsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem == newItem
    }
}
