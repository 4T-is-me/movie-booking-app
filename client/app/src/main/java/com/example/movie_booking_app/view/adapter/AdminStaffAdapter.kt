package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.EmployeeItem
import com.example.movie_booking_app.databinding.ItemAdminEmployeeBinding

class AdminStaffAdapter(
    private val onDetailClick: (EmployeeItem) -> Unit,
    private val onEditClick: (EmployeeItem) -> Unit,
    private val onToggleStatusClick: (EmployeeItem) -> Unit
) : ListAdapter<EmployeeItem, AdminStaffAdapter.StaffViewHolder>(StaffDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemAdminEmployeeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffViewHolder(private val binding: ItemAdminEmployeeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(staff: EmployeeItem) {
            binding.tvEmployeeName.text = staff.fullName ?: "-"
            binding.tvEmployeeEmail.text = staff.account?.email ?: staff.phone ?: "No contact info"
            
            val status = staff.account?.status ?: 1
            val isActive = status != 0
            
            binding.chipStatus.text = if (isActive) "Active" else "Locked"
            val statusColor = if (isActive) R.color.color_green_status else R.color.text_gray
            binding.chipStatus.setChipBackgroundColorResource(statusColor)
            
            binding.chipRole.text = staff.position ?: "Staff"

            binding.btnEditEmployee.setOnClickListener {
                onEditClick(staff)
            }
            
            // Toggle Status Button: Sử dụng icon đồng bộ với Member list
            binding.btnToggleStatus.visibility = android.view.View.VISIBLE
            binding.btnToggleStatus.setImageResource(android.R.drawable.ic_lock_power_off)
            
            // Đổi màu icon phản ánh trạng thái hiện tại: Active -> Xanh, Locked -> Đỏ
            binding.btnToggleStatus.setColorFilter(
                ContextCompat.getColor(binding.root.context, 
                    if (isActive) R.color.color_green_status else R.color.deduct
                )
            )

            binding.btnToggleStatus.setOnClickListener {
                onToggleStatusClick(staff)
            }

            binding.root.setOnClickListener {
                onDetailClick(staff)
            }

            Glide.with(binding.ivEmployeeAvatar.context)
                .load(R.drawable.ic_profile)
                .into(binding.ivEmployeeAvatar)
        }
    }

    class StaffDiffCallback : DiffUtil.ItemCallback<EmployeeItem>() {
        override fun areItemsTheSame(oldItem: EmployeeItem, newItem: EmployeeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EmployeeItem, newItem: EmployeeItem): Boolean {
            return oldItem == newItem
        }
    }
}
