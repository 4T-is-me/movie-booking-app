package com.example.movie_booking_app.view.adapter

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room)
        holder.itemView.setOnClickListener { onRoomClick(room) }
    }

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvTotalSeats: TextView = itemView.findViewById(R.id.tvTotalSeats)
        private val tvRoomHint: TextView = itemView.findViewById(R.id.tvRoomHint)

        fun bind(room: CinemaRoom) {
            tvRoomName.text = room.roomName
            tvTotalSeats.text = "Total Seats: ${room.totalSeats}"
            tvRoomHint.text = "Tap to edit seat layout"
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<CinemaRoom>() {
        override fun areItemsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem == newItem
    }
}
