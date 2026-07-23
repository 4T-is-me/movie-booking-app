package com.example.movie_booking_app.view.adapter.manager

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.DaySchedule
import com.example.movie_booking_app.databinding.ItemDayScheduleBinding

class WeekScheduleAdapter(private var list: List<DaySchedule> = emptyList()) :
    RecyclerView.Adapter<WeekScheduleAdapter.ViewHolder>() {

    fun submitList(newList: List<DaySchedule>) {
        val diffCallback = WeekScheduleDiffCallback(this.list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(private val binding: ItemDayScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DaySchedule) {
            val context = binding.root.context

            binding.tvDayOfWeek.text = item.day
            binding.tvDate.text = item.date

            val shiftText = item.shift

            // Logic check shift
            if (shiftText.isNullOrEmpty() || shiftText.equals("Day Off", ignoreCase = true)) {
                // --- DAY OFF ---
                binding.tvShiftName.text = context.getString(R.string.auto_str_day_off)
                binding.tvDayOffBadge.visibility = View.VISIBLE

                binding.layoutIconBg.setBackgroundResource(R.drawable.bg_shift_item)
                binding.ivShiftIcon.setImageResource(R.drawable.ic_event)
                binding.ivShiftIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )

            } else {
                // --- HAS SHIFT ---
                binding.tvShiftName.text = shiftText
                binding.tvDayOffBadge.visibility = View.GONE
                val firstShift = shiftText.split("-").firstOrNull()?.trim() ?: ""

                // check first shift Evening/Night or Morning/Afternoon
                val isNightOrEveningFirst = firstShift.equals("Evening", ignoreCase = true) ||
                        firstShift.equals("Night", ignoreCase = true)

                if (isNightOrEveningFirst) {
                    // Evening / Night -> Theme moon
                    binding.layoutIconBg.setBackgroundResource(R.drawable.bg_icon_night)
                    binding.ivShiftIcon.setImageResource(R.drawable.ic_moon)
                    binding.ivShiftIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.tint_night_purple)
                    )
                } else {
                    // Morning / Afternoon -> Theme sun
                    binding.layoutIconBg.setBackgroundResource(R.drawable.bg_icon_sun)
                    binding.ivShiftIcon.setImageResource(R.drawable.ic_sun)
                    binding.ivShiftIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.tint_sun_yellow)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}

class WeekScheduleDiffCallback(private val oldList: List<DaySchedule>, private val newList: List<DaySchedule>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].day == newList[newItemPosition].day
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}