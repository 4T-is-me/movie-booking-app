package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemCinemaRoomBinding
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
    private val onRoomClick: (CinemaRoom) -> Unit,
    private val onEditClick: (CinemaRoom) -> Unit,
    private val onToggleStatusClick: (CinemaRoom) -> Unit
) : ListAdapter<CinemaRoom, CinemaRoomsAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemCinemaRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding, onRoomClick, onEditClick, onToggleStatusClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = getItem(position)
        holder.bind(room)
    }

    class RoomViewHolder(private val binding: ItemCinemaRoomBinding, private val onRoomClick: (CinemaRoom) -> Unit,
        private val onEditClick: (CinemaRoom) -> Unit,
        private val onToggleStatusClick: (CinemaRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val tvRoomName: TextView = binding.tvRoomName
        private val tvStatusChip: TextView = binding.tvStatusChip
        private val tvRoomType: TextView = binding.tvRoomType
        private val tvCapacity: TextView = binding.tvCapacity
        private val ivOptions: View = binding.ivOptions

        fun bind(room: CinemaRoom) {
            tvRoomName.text = room.roomName
            
            // Format type: e.g., Standard • 2D
            val formattedType = "${room.roomType ?: "Standard"} • ${room.screenType ?: "2D"}"
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

            itemView.setOnClickListener { onEditClick(room) }

            ivOptions.setOnClickListener { view ->
                val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Edit Room")
                if (status.lowercase() == "active") {
                    popup.menu.add(0, 2, 1, "Close Room")
                } else {
                    popup.menu.add(0, 3, 1, "Reactivate Room")
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> {
                            onEditClick(room)
                            true
                        }
                        2, 3 -> {
                            onToggleStatusClick(room)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<CinemaRoom>() {
        override fun areItemsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CinemaRoom, newItem: CinemaRoom): Boolean = oldItem == newItem
    }
}
