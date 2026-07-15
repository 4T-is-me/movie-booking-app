package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.ItemAdminMemberBinding
import com.example.movie_booking_app.utils.AuthUtils
import com.bumptech.glide.Glide

class MemberAdapter(
    private val onAdjustScore: (MemberItem) -> Unit,
    private val onToggleStatus: (MemberItem) -> Unit
) : ListAdapter<MemberItem, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemAdminMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemAdminMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MemberItem) {
            binding.tvMemberName.text = item.fullName ?: "Unknown"
            binding.tvMemberEmail.text = item.account?.email ?: "No email"
            binding.tvMemberScore.text = String.format("%,d", item.rewardPoints ?: 0)
            
            val isActive = item.account?.status != 0
            binding.chipMemberStatus.text = if (isActive) "Active" else "Locked"
            binding.chipMemberStatus.setChipBackgroundColorResource(if (isActive) android.R.color.holo_green_dark else android.R.color.darker_gray)
            
            binding.btnAdjustScore.setOnClickListener { onAdjustScore(item) }
            
            // Đồng bộ màu sắc phản ánh trạng thái hiện tại
            binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_lock_power_off)
            binding.btnToggleMemberStatus.setColorFilter(
                androidx.core.content.ContextCompat.getColor(binding.root.context,
                    if (isActive) R.color.color_green_status else R.color.deduct
                )
            )
            binding.btnToggleMemberStatus.setOnClickListener { onToggleStatus(item) }
            
            Glide.with(binding.ivMemberAvatar.context)
                .load(item.image)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(binding.ivMemberAvatar)
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<MemberItem>() {
        override fun areItemsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem == newItem
    }
}
