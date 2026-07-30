package com.example.movie_booking_app.view.adapter.manager

import androidx.core.content.ContextCompat

import com.example.movie_booking_app.databinding.ItemManagerRoomBinding
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
        val binding = ItemManagerRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding, onRoomClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoomViewHolder(private val binding: ItemManagerRoomBinding, private val onRoomClick: (ManagerRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val tvRoomName: TextView = binding.tvRoomName
        private val tvTotalSeats: TextView = binding.tvTotalSeats
        private val tvStdCount: TextView = binding.tvStdCount
        private val tvVipCount: TextView = binding.tvVipCount
        private val tvCoupleCount: TextView = binding.tvCoupleCount
        private val tvOperating: TextView = binding.tvOperating
        private val tvRoomUnavailable: TextView = binding.tvRoomUnavailable
        private val flIcon: View = binding.flIcon
        private val ivMovie: android.widget.ImageView = binding.ivMovie
        private val btnManage: View = binding.btnManage

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
                    tvOperating.setTextColor(ContextCompat.getColor(context, R.color.color_green))
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_operating)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_manager_room_icon)
                    ivMovie.setColorFilter(ContextCompat.getColor(context, R.color.color_green))
                    
                    tvRoomUnavailable.visibility = View.GONE
                }
                "Maintenance" -> {
                    tvOperating.setTextColor(ContextCompat.getColor(context, R.color.auto_color_F59E0B)) // Orange yellow
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_maintain)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_maintain)
                    ivMovie.setColorFilter(ContextCompat.getColor(context, R.color.auto_color_F59E0B))
                    
                    tvRoomUnavailable.visibility = View.VISIBLE
                    tvRoomUnavailable.text = itemView.context.getString(R.string.auto_str_room_unavailable_tap_to_manage)
                    tvRoomUnavailable.setTextColor(ContextCompat.getColor(context, R.color.auto_color_F59E0B))
                }
                "Closed" -> {
                    tvOperating.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
                    tvOperating.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_close)
                    
                    flIcon.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_label_close)
                    ivMovie.setColorFilter(ContextCompat.getColor(context, R.color.color_error_red))
                    
                    tvRoomUnavailable.visibility = View.VISIBLE
                    tvRoomUnavailable.text = itemView.context.getString(R.string.auto_str_room_closed_tap_to_manage)
                    tvRoomUnavailable.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
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
