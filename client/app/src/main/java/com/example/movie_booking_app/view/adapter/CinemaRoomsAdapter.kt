package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.databinding.ItemCinemaRoomBinding

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
            val formattedType = itemView.context.getString(R.string.admin_room_type_format, room.roomType ?: "Standard", room.screenType ?: "2D")
            tvRoomType.text = formattedType

            // Format capacity: e.g., 120 seats
            tvCapacity.text = itemView.context.getString(R.string.admin_room_seats_count, room.totalSeats)

            // Adjust status chip presentation using existing drawables
            val status = room.status ?: "active"
            val statusText: String
            val textColor: Int
            val backgroundRes: Int

            when (status.lowercase()) {
                "active", "operating", "open" -> {
                    statusText = itemView.context.getString(R.string.status_active_en)
                    textColor = ContextCompat.getColor(itemView.context, R.color.color_green)
                    backgroundRes = R.drawable.bg_label_operating
                }
                "maintenance", "maintain" -> {
                    statusText = itemView.context.getString(R.string.status_maintenance_en)
                    textColor = ContextCompat.getColor(itemView.context, R.color.auto_color_F59E0B)
                    backgroundRes = R.drawable.bg_label_maintain
                }
                else -> {
                    statusText = itemView.context.getString(R.string.status_closed_en)
                    textColor = ContextCompat.getColor(itemView.context, R.color.color_error_red)
                    backgroundRes = R.drawable.bg_label_close
                }
            }

            tvStatusChip.text = statusText
            tvStatusChip.setTextColor(textColor)
            tvStatusChip.background = ContextCompat.getDrawable(itemView.context, backgroundRes)

            itemView.setOnClickListener { onEditClick(room) }

            ivOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, view.context.getString(R.string.admin_room_edit))
                if (status.lowercase() == "active") {
                    popup.menu.add(0, 2, 1, view.context.getString(R.string.admin_room_close))
                } else {
                    popup.menu.add(0, 3, 1, view.context.getString(R.string.admin_room_reactivate))
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
