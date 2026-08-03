package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.PayrollItem
import com.example.movie_booking_app.databinding.ItemPayrollBinding
import com.example.movie_booking_app.utils.NameFormatter
import java.text.NumberFormat
import java.util.Locale

class PayrollAdapter(
    private val onItemClick: (PayrollItem) -> Unit
) : ListAdapter<PayrollItem, PayrollAdapter.PayrollViewHolder>(PayrollDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayrollViewHolder {
        val binding = ItemPayrollBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PayrollViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PayrollViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PayrollViewHolder(
        private val binding: ItemPayrollBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PayrollItem) {
            val ctx = binding.root.context

            // Employee name
            binding.tvEmployeeName.text = item.employee?.fullName
                ?: item.employee?.username
                ?: ctx.getString(R.string.user_name_placeholder)

            // Role
            binding.tvEmployeeRole.text = item.employee?.role ?: ctx.getString(R.string.role_staff)

            // Cinema
            binding.tvCinemaName.text = item.cinema?.cinemaName ?: ""

            // Avatar Text Initials
            val fullName = item.employee?.fullName ?: item.employee?.username ?: ""
            binding.tvAvatarText.text = NameFormatter.getAvatarInitials(fullName)

            // Total salary – VND format
            binding.tvTotalSalary.text = formatVnd(item.totalSalary)

            // Status badge
            val isDraft = item.status.equals("Draft", ignoreCase = true)
            if (isDraft) {
                binding.tvStatus.text = ctx.getString(R.string.status_draft)
                binding.tvStatus.setTextColor(ctx.getColor(R.color.color_draft_status))
                binding.tvStatus.backgroundTintList = ctx.getColorStateList(R.color.color_draft_status_bg)
            } else {
                binding.tvStatus.text = ctx.getString(R.string.status_paid)
                binding.tvStatus.setTextColor(ctx.getColor(R.color.color_paid_status))
                binding.tvStatus.backgroundTintList = ctx.getColorStateList(R.color.color_paid_status_bg)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class PayrollDiffCallback : DiffUtil.ItemCallback<PayrollItem>() {
        override fun areItemsTheSame(oldItem: PayrollItem, newItem: PayrollItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PayrollItem, newItem: PayrollItem): Boolean =
            oldItem == newItem
    }

    companion object {
        fun formatVnd(amount: Double): String {
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            return "${formatter.format(amount.toLong())} VND"
        }
    }
}
