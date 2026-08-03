package com.example.movie_booking_app.view.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatType
import com.example.movie_booking_app.databinding.ItemAdminSeatBinding

class AdminSeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, AdminSeatAdapter.SeatViewHolder>(SeatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemAdminSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = getItem(position)
        holder.bind(seat)
        holder.itemView.setOnClickListener { onSeatClick(seat) }
    }

    class SeatViewHolder(private val binding: ItemAdminSeatBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvSeatCode: TextView = binding.tvSeatCode

        fun bind(seat: Seat) {
            tvSeatCode.text = "${seat.row}${seat.number}"
            
            val color = if (seat.type == SeatType.VIP) {
                ContextCompat.getColor(itemView.context, R.color.yellow)
            } else {
                ContextCompat.getColor(itemView.context, R.color.nav_inactive)
            }
            tvSeatCode.setBackgroundColor(color)
            
            // Text color based on background
            tvSeatCode.setTextColor(if (seat.type == SeatType.VIP) Color.BLACK else Color.WHITE)
        }
    }

    class SeatDiffCallback : DiffUtil.ItemCallback<Seat>() {
        override fun areItemsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem == newItem
    }
}
