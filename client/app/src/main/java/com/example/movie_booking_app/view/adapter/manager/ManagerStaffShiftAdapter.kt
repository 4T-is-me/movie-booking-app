package com.example.movie_booking_app.view.adapter.manager

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.ItemStaffCardBinding
import com.example.movie_booking_app.utils.Constants
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ManagerStaffShiftAdapter(
    private var list: List<ShiftItemResponse> = emptyList(),
    private val onItemClick: ((ShiftItemResponse) -> Unit)? = null
): RecyclerView.Adapter<ManagerStaffShiftAdapter.ShiftViewHolder>() {
    inner class ShiftViewHolder(val binding: ItemStaffCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftViewHolder {
        val binding = ItemStaffCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShiftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShiftViewHolder, position: Int) {
        val shiftItem = list[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvStaffName.text = shiftItem.staffName ?: ""
        binding.tvAvatarText.text = getAvatarInitials(shiftItem.staffName ?: "")

        val positionText = shiftItem.position ?: "Staff"
        val shiftName = shiftItem.shift ?: ""
        val shiftTime = getShiftTimeDisplay(shiftName)

        binding.tvPosition.text = positionText


        binding.tvWorkDate.text = "Date: ${formatDate(shiftItem.workDate)}"
        binding.tvShift.text = "Shift: $shiftName ($shiftTime)"

        val rating = shiftItem.rating ?: 0.0
        if (rating > 0) {
            binding.tvStaffRating.visibility = View.VISIBLE
            binding.tvStaffRating.text = "⭐ ${String.format("%.1f", rating)}"
        } else {
            binding.tvStaffRating.visibility = View.GONE
        }

        // 4. Status Badge (Active / Assigned / Completed / Absent)
        when(shiftItem.status) {
            "Assigned" -> {
                binding.tvStatusBadge.text = "Assigned"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active_text)) // green
                binding.tvStatusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_active_bg))
            }
            "Completed" -> {
                binding.tvStatusBadge.text = "Completed"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_complete_text)) // yellow Gold
                binding.tvStatusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_complete_bg))
            }
            "Absent" -> {
                binding.tvStatusBadge.text = "Absent"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_leave_text)) // red
                binding.tvStatusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_active_bg))
            }
            else -> {
                binding.tvStatusBadge.text = "Active"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_active_text)) // green
                binding.tvStatusBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_active_bg))
            }
        }

        binding.root.setOnClickListener {
            onItemClick?.invoke(shiftItem)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateData(newList: List<ShiftItemResponse>) {
        this.list = newList
        notifyDataSetChanged()
    }

    fun getAvatarInitials(staffName: String) : String {
        val name = staffName.trim()
        if (name.isEmpty()) return "NV"

        val parts = name.split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
            else -> name.take(2).uppercase()
        }
    }

    private fun getShiftTimeDisplay(shift: String): String {
        return when (shift) {
            "Morning" -> Constants.MORNING
            "Afternoon" -> Constants.AFTERNOON
            "Evening" -> Constants.EVENING
            "Night" -> Constants.NIGHT
            else -> Constants.FULLTIME
        }
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val parser = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            )
            parser.timeZone = TimeZone.getTimeZone("UTC")

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("vi", "VN")
            )
            formatter.timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

            formatter.format(parser.parse(dateString)!!)
        } catch (e: Exception) {
            dateString
        }
    }
}