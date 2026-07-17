package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerRoom

class ManagerRoomAdapter(
    private val onRoomClick: (ManagerRoom) -> Unit
) : ListAdapter<ManagerRoom, ManagerRoomAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manager_room, parent, false)
        return RoomViewHolder(view, onRoomClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoomViewHolder(
        itemView: View,
        private val onRoomClick: (ManagerRoom) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvTotalSeats: TextView = itemView.findViewById(R.id.tvTotalSeats)
        private val tvStdCount: TextView = itemView.findViewById(R.id.tvStdCount)
        private val tvVipCount: TextView = itemView.findViewById(R.id.tvVipCount)
        private val tvCoupleCount: TextView = itemView.findViewById(R.id.tvCoupleCount)
        private val tvOperating: TextView = itemView.findViewById(R.id.tvOperating)
        private val tvRoomUnavailable: TextView = itemView.findViewById(R.id.tvRoomUnavailable)
        private val flIcon: View = itemView.findViewById(R.id.flIcon)
        private val ivMovie: android.widget.ImageView = itemView.findViewById(R.id.ivMovie)
        private val btnManage: View = itemView.findViewById(R.id.btnManage)

        fun bind(room: ManagerRoom) {
            tvRoomName.text = room.roomName
            tvTotalSeats.text = "${room.totalSeats} seats · ${room.roomType ?: "Standard"}"
            
            tvStdCount.text = room.regularSeats.toString()
            tvVipCount.text = room.vipSeats.toString()
            tvCoupleCount.text = room.coupleSeats.toString()
            
            // Handle status
            val statusText = when (room.status?.lowercase()) {
                "active", "operating", "open" -> "Operating"
                "maintenance", "maintain" -> "Maintenance"
                else -> "Closed"
            }
            tvOperating.text = statusText
            
            // Change color by status
            val context = itemView.context
            when (statusText) {
                "Operating" -> {
                    tvOperating.setTextColor(android.graphics.Color.parseColor("#10B981"))
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_operating)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_manager_room_icon)
                    ivMovie.setColorFilter(android.graphics.Color.parseColor("#10B981"))
                    
                    tvRoomUnavailable.visibility = View.GONE
                }
                "Maintenance" -> {
                    tvOperating.setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Orange yellow
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_maintain)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_maintain)
                    ivMovie.setColorFilter(android.graphics.Color.parseColor("#F59E0B"))
                    
                    tvRoomUnavailable.visibility = View.VISIBLE
                    tvRoomUnavailable.text = "⚠️ Room unavailable · Tap to manage"
                    tvRoomUnavailable.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                }
                "Closed" -> {
                    tvOperating.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_close)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_close)
                    ivMovie.setColorFilter(android.graphics.Color.parseColor("#EF4444"))
                    
                    tvRoomUnavailable.visibility = View.VISIBLE
                    tvRoomUnavailable.text = "⚠️ Room closed · Tap to manage"
                    tvRoomUnavailable.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                }
            }
            
            btnManage.setOnClickListener {
                onRoomClick(room)
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<ManagerRoom>() {
        override fun areItemsTheSame(oldItem: ManagerRoom, newItem: ManagerRoom): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ManagerRoom, newItem: ManagerRoom): Boolean {
            return oldItem == newItem
        }
    }
}
