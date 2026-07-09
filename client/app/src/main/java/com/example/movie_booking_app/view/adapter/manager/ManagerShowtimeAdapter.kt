package com.example.movie_booking_app.view.adapter.manager

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ShowtimeTodayResponse
import com.example.movie_booking_app.databinding.ItemManagerShowtimeBinding

class ManagerShowtimeAdapter : ListAdapter<ShowtimeTodayResponse, ManagerShowtimeAdapter.ShowtimeViewHolder>(ShowtimeDiffCallback()) {
    override fun onCreateViewHolder( parent: ViewGroup, viewType: Int ): ShowtimeViewHolder {
        val binding = ItemManagerShowtimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShowtimeViewHolder(binding)
    }

    override fun onBindViewHolder( holder: ShowtimeViewHolder, position: Int ) {
        holder.bind(getItem(position))
    }

    class ShowtimeViewHolder(private val binding: ItemManagerShowtimeBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShowtimeTodayResponse) {
            val context = binding.root.context
            binding.tvMovieName.text = item.movieName
            binding.tvRoomAndTime.text = "${item.roomName} · ${item.startTime} - ${item.endTime}"
            binding.tvSeatsCount.text = "${item.bookedSeats}/${item.totalSeats} seats"

            val rateInt = item.occupancyRate.toInt()
            binding.tvOccupancyPercentage.text = "$rateInt%"
            binding.pbOccupancy.progress = rateInt
            binding.tvStatus.text = item.status

            // Màu sắc động theo trạng thái (Live: Xanh, Done: Xám/vàng tối)
            if (item.status.equals("Live", ignoreCase = true)) {
                // Đèn xanh cho trạng thái đang chiếu
                val greenColor = ContextCompat.getColor(context, R.color.color_green_status)
                val greenBgColor = ContextCompat.getColor(context, R.color.color_green_status_bg)

                binding.viewDot.backgroundTintList = ColorStateList.valueOf(greenColor)
                binding.statusContainer.backgroundTintList = ColorStateList.valueOf(greenBgColor)
                binding.tvStatus.setTextColor(greenColor)
                binding.lineVertical.backgroundTintList = ColorStateList.valueOf(greenColor)
            }else{
                // Trạng thái Done hoặc trạng thái khác
                val grayColor = ContextCompat.getColor(context, R.color.color_gray_status)
                val grayBgColor = ContextCompat.getColor(context, R.color.color_gray_status_bg)

                binding.lineVertical.backgroundTintList = ColorStateList.valueOf(grayColor)
                binding.viewDot.backgroundTintList = ColorStateList.valueOf(grayColor)
                binding.statusContainer.backgroundTintList = ColorStateList.valueOf(grayBgColor)
                binding.tvStatus.setTextColor(grayColor)

            }
        }
    }

    class ShowtimeDiffCallback : DiffUtil.ItemCallback<ShowtimeTodayResponse>() {
        override fun areItemsTheSame( oldItem: ShowtimeTodayResponse, newItem: ShowtimeTodayResponse  ): Boolean {
            return oldItem.showtimeId == newItem.showtimeId
        }

        override fun areContentsTheSame( oldItem: ShowtimeTodayResponse,  newItem: ShowtimeTodayResponse ): Boolean {
            return oldItem == newItem
        }
    }
}