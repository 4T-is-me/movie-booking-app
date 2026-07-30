package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.NotificationItemResponse
import com.example.movie_booking_app.databinding.ItemNotificationBinding
import com.example.movie_booking_app.utils.DateUtils

class StaffNotifAdapter(
    private val onItemClick: (NotificationItemResponse) -> Unit
) : ListAdapter<NotificationItemResponse, StaffNotifAdapter.NotifViewHolder>(DiffCallback) {

    inner class NotifViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItemResponse) {
            val date = DateUtils.formatMovieDate(item.createdAt)
            val time = DateUtils.formatMovieTime(item.createdAt)
            with(binding) {
                tvNotifTitle.text = item.title
                tvNotifBody.text = item.message
                tvNotifTime.text = "${date} - ${time}"

                // Chỉ hiện chấm indicator nếu chưa đọc, giữ nền giống user (dùng màu từ XML)
                viewUnreadIndicator.visibility = if (item.isRead) View.GONE else View.VISIBLE

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationItemResponse>() {
            override fun areItemsTheSame(oldItem: NotificationItemResponse, newItem: NotificationItemResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: NotificationItemResponse, newItem: NotificationItemResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}