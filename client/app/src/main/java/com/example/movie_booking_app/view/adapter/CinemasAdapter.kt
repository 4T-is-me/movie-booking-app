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
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.databinding.ItemCinemaRoomBinding

class CinemasAdapter(
    private val onCinemaClick: (Cinema) -> Unit,
    private val onEditClick: (Cinema) -> Unit,
    private val onToggleStatusClick: (Cinema) -> Unit
) : ListAdapter<Cinema, CinemasAdapter.CinemaViewHolder>(CinemaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val binding = ItemCinemaRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CinemaViewHolder(binding, onCinemaClick, onEditClick, onToggleStatusClick)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CinemaViewHolder(private val binding: ItemCinemaRoomBinding, private val onClick: (Cinema) -> Unit,
        private val onEdit: (Cinema) -> Unit,
        private val onToggleStatus: (Cinema) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val tvRoomName: TextView = binding.tvRoomName
        private val tvStatusChip: TextView = binding.tvStatusChip
        private val tvCapacity: TextView = binding.tvCapacity
        private val tvRoomType: TextView = binding.tvRoomType
        private val ivOptions: View = binding.ivOptions

        fun bind(cinema: Cinema) {
            tvRoomName.text = cinema.cinemaName ?: "N/A"
            
            val status = cinema.status?.lowercase() ?: "active"
            val statusText: String
            val statusColor: Int
            val backgroundRes: Int

            if (status == "active") {
                statusText = "Active"
                statusColor = ContextCompat.getColor(itemView.context, R.color.color_green)
                backgroundRes = R.drawable.bg_label_operating
            } else {
                statusText = "Closed"
                statusColor = ContextCompat.getColor(itemView.context, R.color.color_error_red)
                backgroundRes = R.drawable.bg_label_close
            }

            tvStatusChip.text = statusText
            tvStatusChip.setTextColor(statusColor)
            tvStatusChip.background = ContextCompat.getDrawable(itemView.context, backgroundRes)

            val totalRooms = cinema.totalRooms ?: 0
            tvCapacity.text = if (totalRooms == 1) "1 room" else "$totalRooms rooms"
            tvRoomType.text = "${cinema.address ?: ""}, ${cinema.city ?: ""}"

            itemView.setOnClickListener { onClick(cinema) }

            ivOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Edit Cinema")
                if (status == "active") {
                    popup.menu.add(0, 2, 1, "Close Cinema")
                } else {
                    popup.menu.add(0, 3, 1, "Reactivate Cinema")
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> {
                            onEdit(cinema)
                            true
                        }
                        2, 3 -> {
                            onToggleStatus(cinema)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class CinemaDiffCallback : DiffUtil.ItemCallback<Cinema>() {
        override fun areItemsTheSame(oldItem: Cinema, newItem: Cinema): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Cinema, newItem: Cinema): Boolean {
            return oldItem == newItem
        }
    }
}
