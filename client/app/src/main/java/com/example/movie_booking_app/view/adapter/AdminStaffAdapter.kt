package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.databinding.ItemAdminStaffBinding

class AdminStaffAdapter : ListAdapter<Staff, AdminStaffAdapter.StaffViewHolder>(StaffDiffCallback()) {

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
            binding.tvStaffUsername.text = staff.username ?: "-"
            binding.tvStaffFullName.text = staff.fullName ?: "-"
            binding.tvStaffDob.text = staff.dob ?: "-"
            binding.tvStaffGender.text = staff.gender ?: "-"
            binding.tvStaffEmail.text = staff.email ?: "-"
            binding.tvStaffIdCard.text = staff.identityCard ?: "-"
            binding.tvStaffPhone.text = staff.phone ?: "-"
            binding.tvStaffAddress.text = staff.address ?: "-"
        }
    }

    class StaffDiffCallback : DiffUtil.ItemCallback<Staff>() {
        override fun areItemsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem.username == newItem.username
        }

        override fun areContentsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem == newItem
        }
    }
}
