package com.example.movie_booking_app.view.adapter.manager

import com.example.movie_booking_app.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.databinding.ItemGeneratedScheduleBinding
import com.example.movie_booking_app.utils.ShowtimeDraft
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AutoSchedulingAdapter : RecyclerView.Adapter<AutoSchedulingAdapter.ViewHolder>() {

    private val scheduleList = mutableListOf<ShowtimeDraft>()

    fun submitList(list: List<ShowtimeDraft>) {
        scheduleList.clear()
        scheduleList.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemGeneratedScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShowtimeDraft) {
            binding.tvMovieTitle.text = item.movie_name
            
            try {
                val start = LocalDateTime.parse(item.start_time, DateTimeFormatter.ISO_DATE_TIME)
                val end = LocalDateTime.parse(item.end_time, DateTimeFormatter.ISO_DATE_TIME)
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                binding.tvSubtitle.text = binding.root.context.getString(R.string.auto_schedule_format, item.room_name, start.format(timeFormatter), end.format(timeFormatter))
            } catch (e: Exception) {
                binding.tvSubtitle.text = binding.root.context.getString(R.string.auto_schedule_format_fallback, item.room_name, item.start_time)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGeneratedScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = scheduleList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scheduleList[position])
    }
}
