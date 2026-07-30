package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ReplacementShiftResponse
import com.example.movie_booking_app.databinding.ItemReplacementShiftBinding
import com.example.movie_booking_app.utils.Constants
import java.text.SimpleDateFormat
import java.util.Locale

class ReplacementShiftAdapter(private val onAcceptClick: (ReplacementShiftResponse) -> Unit
):  ListAdapter<ReplacementShiftResponse, ReplacementShiftAdapter.ViewHolder>(DiffCallback) {
    inner class ViewHolder(private val binding: ItemReplacementShiftBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReplacementShiftResponse) {
            val context = itemView.context
            val dateStr = item.workDate ?: ""
            val (dayNum, monthName) = parseDateString(dateStr)
            val dayOfWeek = getDayOfWeek(dateStr)

            binding.tvBadgeDayName.text = dayOfWeek
            binding.tvBadgeDayNumber.text = dayNum
            binding.tvBadgeMonthName.text = monthName

            binding.tvReplaceShiftName.text = "${item.shift ?: "Flexible"} Shift"
            binding.tvReplaceStaffName.text = item.staffName ?: "Unknown Staff"
            binding.tvReplacePosition.text = item.staffPosition ?: "Staff"

            binding.tvShiftTimeText.text = when (item.shift) {
                Constants.SHIFTS[0] -> Constants.MORNING
                Constants.SHIFTS[1]-> Constants.AFTERNOON
                Constants.SHIFTS[2]-> Constants.EVENING
                Constants.SHIFTS[3]-> Constants.NIGHT
                else -> Constants.FULLTIME
            }


            binding.tvShiftStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_gold)
            binding.tvShiftStatus.setTextColor(ContextCompat.getColor(context, R.color.color_gold))


            item.approvedAt?.let { approvedDate ->
                val cleanApproved =
                    if (approvedDate.contains("T")) approvedDate.substringBefore("T") else approvedDate
                binding.tvApprovedAt.text = "Approved: $cleanApproved"
            } ?: run {
                binding.tvApprovedAt.text = getString(R.string.auto_str_approved)
            }

            binding.btnAcceptShift.setOnClickListener {
                onAcceptClick(item)
            }
        }
    }
    private fun parseDateString(dateStr: String): Pair<String, String> {
        val cleanDate = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
        val parts = cleanDate.split("-")
        if (parts.size != 3) return Pair("", "")
        val monthName = when (parts[1]) {
            "01" -> "Jan" "02" -> "Feb" "03" -> "Mar" "04" -> "Apr" "05" -> "May" "06" -> "Jun"
            "07" -> "Jul" "08" -> "Aug" "09" -> "Sep" "10" -> "Oct" "11" -> "Nov" "12" -> "Dec"
            else -> "Jul"
        }
        return Pair(parts[2], monthName)
    }
    private fun getDayOfWeek(dateStr: String): String {
        val cleanDate = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(cleanDate)
            if (date != null) SimpleDateFormat("EEE", Locale.ENGLISH).format(date).uppercase() else "MON"
        } catch (e: Exception) { "MON" }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReplacementShiftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ReplacementShiftResponse>() {

        override fun areItemsTheSame(oldItem: ReplacementShiftResponse, newItem: ReplacementShiftResponse): Boolean {
            return oldItem.leaveRequestId == newItem.leaveRequestId
        }

        override fun areContentsTheSame(oldItem: ReplacementShiftResponse, newItem: ReplacementShiftResponse): Boolean {
            return oldItem == newItem
        }
    }
}