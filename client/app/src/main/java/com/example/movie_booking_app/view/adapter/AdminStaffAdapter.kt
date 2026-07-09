package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.databinding.ItemAdminStaffBinding

class AdminStaffAdapter(
    private val onDetailClick: (Staff) -> Unit,
    private val onEditClick: (Staff) -> Unit,
    private val onDeleteClick: (Staff) -> Unit
) : ListAdapter<Staff, AdminStaffAdapter.StaffViewHolder>(StaffDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemAdminStaffBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffViewHolder(private val binding: ItemAdminStaffBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(staff: Staff) {
            binding.tvStaffFullName.text = staff.fullName ?: "-"
            binding.tvStaffPosition.text = staff.position ?: "-"

            binding.btnDetailStaff.setOnClickListener {
                onDetailClick(staff)
            }
            binding.btnEditStaff.setOnClickListener {
                onEditClick(staff)
            }
            binding.btnDeleteStaff.setOnClickListener {
                onDeleteClick(staff)
            }
        }
    }

    class StaffDiffCallback : DiffUtil.ItemCallback<Staff>() {
        override fun areItemsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem == newItem
        }
    }
}
