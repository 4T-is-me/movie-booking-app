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
    private val seats: List<ManagerSeat>,
    private val selectedSeatIds: Set<String> = emptySet(),
    private val onSeatClick: ((ManagerSeat) -> Unit)? = null,
    private val onSeatLongClick: ((ManagerSeat) -> Unit)? = null
) : RecyclerView.Adapter<ManagerSeatAdapter.SeatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemManagerSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.bind(seat, selectedSeatIds, onSeatClick, onSeatLongClick)
    }

    override fun getItemCount(): Int = seats.size

    class SeatViewHolder(private val binding: ItemManagerSeatBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvSeatName: View = binding.tvSeatName

        fun bind(
            seat: ManagerSeat,
            selectedSeatIds: Set<String>,
            onSeatClick: ((ManagerSeat) -> Unit)?,
            onSeatLongClick: ((ManagerSeat) -> Unit)?
        ) {
            if (selectedSeatIds.contains(seat.id)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_selected)
                tvSeatName.alpha = 1f
            } else if (seat.status.equals("maintenance", ignoreCase = true)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_maintenance)
                tvSeatName.alpha = 1f
            } else if (seat.seatType.equals("VIP", ignoreCase = true)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_vip)
                tvSeatName.alpha = if (seat.status == "0" || seat.status.equals("inactive", ignoreCase = true)) 0.5f else 1f
            } else if (seat.seatType.equals("Couple", ignoreCase = true)) {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_couple)
                tvSeatName.alpha = if (seat.status == "0" || seat.status.equals("inactive", ignoreCase = true)) 0.5f else 1f
            } else {
                tvSeatName.setBackgroundResource(R.drawable.bg_seat_manager_std)
                tvSeatName.alpha = if (seat.status == "0" || seat.status.equals("inactive", ignoreCase = true)) 0.5f else 1f
            }

            tvSeatName.setOnClickListener {
                onSeatClick?.invoke(seat)
            }

            tvSeatName.setOnLongClickListener {
                onSeatLongClick?.invoke(seat)
                true
            }
        }
    }
}
