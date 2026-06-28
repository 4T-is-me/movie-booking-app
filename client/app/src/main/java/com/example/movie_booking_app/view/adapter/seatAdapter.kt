package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.data.model.entity.SeatType

class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, SeatAdapter.SeatViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = getItem(position)
        holder.bind(seat)
    }

    inner class SeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSeat: TextView = view.findViewById(R.id.tvSeat)

        fun bind(seat: Seat) {
            tvSeat.text = seat.label
            
            val background = when (seat.status) {
                SeatStatus.SELECTED -> R.drawable.bg_seat_selected
                SeatStatus.SOLD -> R.drawable.bg_seat_sold
                SeatStatus.AVAILABLE -> {
                    if (seat.type == SeatType.VIP) R.drawable.bg_seat_vip else R.drawable.bg_seat_available
                }
            }
            tvSeat.setBackgroundResource(background)
            
            // Sold seats are not clickable
            itemView.isEnabled = seat.status != SeatStatus.SOLD
            itemView.alpha = if (seat.status == SeatStatus.SOLD) 0.5f else 1.0f

            itemView.setOnClickListener { onSeatClick(seat) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Seat>() {
        override fun areItemsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem == newItem
    }
}
