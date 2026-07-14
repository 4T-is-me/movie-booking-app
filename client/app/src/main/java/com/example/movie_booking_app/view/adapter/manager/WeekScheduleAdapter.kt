package com.example.movie_booking_app.view.adapter.manager

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.response.DaySchedule
import com.example.movie_booking_app.databinding.ItemDayScheduleBinding

class WeekScheduleAdapter(
    private var list: List<DaySchedule> = emptyList()
) : RecyclerView.Adapter<WeekScheduleAdapter.ViewHolder>() {

    fun submitList(newList: List<DaySchedule>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemDayScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DaySchedule) {
            binding.tvDayName.text = item.day


            if (item.shift.isNullOrEmpty()) {
                // shift null => Day Off
                binding.layoutShiftBox.visibility = View.GONE
                binding.tvDayOff.visibility = View.VISIBLE
            } else {
                // not null -> display shift
                binding.layoutShiftBox.visibility = View.VISIBLE
                binding.tvDayOff.visibility = View.GONE
                binding.tvShiftDetail.text = item.shift
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}