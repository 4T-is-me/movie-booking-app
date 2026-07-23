package com.example.movie_booking_app.view.adapter.manager

import com.example.movie_booking_app.databinding.ItemManagerSeatBinding
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
        val binding = ItemManagerSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.bind(seat)
    }

    override fun getItemCount(): Int = seats.size

    class SeatViewHolder(private val binding: ItemManagerSeatBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvSeatName: View = binding.tvSeatName

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
