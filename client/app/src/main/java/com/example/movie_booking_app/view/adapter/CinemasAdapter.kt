package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Cinema

class CinemasAdapter(
    private val onCinemaClick: (Cinema) -> Unit
) : ListAdapter<Cinema, CinemasAdapter.CinemaViewHolder>(CinemaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cinema_room, parent, false) // Reusing existing layout for speed, but mapped to Cinema
        return CinemaViewHolder(view, onCinemaClick)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CinemaViewHolder(
        itemView: View,
        private val onClick: (Cinema) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tvRoomName)
        private val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val tvRoomType: TextView = itemView.findViewById(R.id.tvRoomType)

        fun bind(cinema: Cinema) {
            tvRoomName.text = cinema.cinemaName ?: "N/A"
            tvStatusChip.text = (cinema.status ?: "active").capitalize()
            tvCapacity.text = "${cinema.city ?: "N/A"}"
            tvRoomType.text = cinema.address ?: "N/A"

            // Status colors
            val statusColor = when (cinema.status?.lowercase()) {
                "active" -> itemView.context.getColor(R.color.auto_color_4CAF50)
                "inactive" -> itemView.context.getColor(R.color.auto_color_F44336)
                else -> itemView.context.getColor(R.color.cinema_primary)
            }
            tvStatusChip.setTextColor(statusColor)

            itemView.setOnClickListener { onClick(cinema) }
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
