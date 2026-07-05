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

class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, SeatAdapter.SeatViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSeat: TextView = view.findViewById(R.id.tvSeat)

        fun bind(seat: Seat) {
            tvSeat.text = seat.label
            tvSeat.setBackgroundResource(seat.getBackgroundResource())
            
            val isReserved = seat.displayStatus == SeatStatus.RESERVED || seat.displayStatus == SeatStatus.HOLDING
            itemView.isEnabled = !isReserved
            itemView.alpha = if (isReserved) 0.5f else 1.0f

            itemView.setOnClickListener { onSeatClick(seat) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Seat>() {
        override fun areItemsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Seat, newItem: Seat): Boolean = 
            oldItem == newItem && oldItem.displayStatus == newItem.displayStatus
    }
}
