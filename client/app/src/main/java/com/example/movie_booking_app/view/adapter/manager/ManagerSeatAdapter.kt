package com.example.movie_booking_app.view.adapter.manager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerSeat

class ManagerSeatAdapter(
    private val seats: List<ManagerSeat>
) : RecyclerView.Adapter<ManagerSeatAdapter.SeatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manager_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.bind(seat)
    }

    override fun getItemCount(): Int = seats.size

    class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSeatName: View = itemView.findViewById(R.id.tvSeatName)

        fun bind(seat: ManagerSeat) {
            
            if (seat.seatType.equals("VIP", ignoreCase = true)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_vip)
            } else if (seat.seatType.equals("Couple", ignoreCase = true)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_couple)
            } else {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_std)
            }
            
            if (seat.status == "0") {
                tvSeatName.alpha = 0.5f
            } else {
                tvSeatName.alpha = 1f
            }
        }
    }
}
