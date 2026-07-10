package com.example.movie_booking_app.view.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.CinemaRoom

class CinemaRoomsAdapter(
    private val onRoomClick: (CinemaRoom) -> Unit
) : ListAdapter<CinemaRoom, CinemaRoomsAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cinema_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room)
        holder.itemView.setOnClickListener { onRoomClick(room) }
    }

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        private val tvRoomType: TextView = itemView.findViewById(R.id.tvRoomType)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)

        fun bind(room: CinemaRoom) {
            tvRoomName.text = room.roomName
            
            // Format type: e.g., Standard • 2D
            val formattedType = "Standard • ${room.roomType ?: "2D"}"
            tvRoomType.text = formattedType

            // Format capacity: e.g., 120 seats
            tvCapacity.text = "${room.totalSeats} seats"

            // Adjust status chip presentation using existing drawables
            val status = room.status ?: "active"
            val statusText: String
            val textColor: Int
            val backgroundRes: Int

            when (status.lowercase()) {
                "active", "operating", "open" -> {
                    statusText = "Active"
                    textColor = Color.parseColor("#10B981")
                    backgroundRes = R.drawable.bg_label_operating
                }
                "maintenance", "maintain" -> {
                    statusText = "Maintenance"
                    textColor = Color.parseColor("#F59E0B")
                    backgroundRes = R.drawable.bg_label_maintain
                }
                else -> {
                    statusText = "Closed"
                    textColor = Color.parseColor("#EF4444")
                    backgroundRes = R.drawable.bg_label_close
                }
            }

            tvStatusChip.text = statusText
            tvStatusChip.setTextColor(textColor)
            tvStatusChip.background = ContextCompat.getDrawable(itemView.context, backgroundRes)
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<CinemaRoom>() {
        override fun areItemsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem == newItem
    }
}
