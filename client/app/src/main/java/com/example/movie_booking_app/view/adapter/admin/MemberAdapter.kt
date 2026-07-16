package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.ItemAdminMemberBinding

class MemberAdapter(
    private val onDetailClick: (MemberItem) -> Unit,
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
            binding.tvMemberPoints.text = String.format("%,d", item.rewardPoints ?: 0)
            
            val isActive = item.account?.status != 0
            
            binding.btnDetailMember.setOnClickListener { onDetailClick(item) }
            binding.btnAdjustScore.setOnClickListener { onAdjustScore(item) }
            
            if (isActive) {
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_delete)
                binding.btnToggleMemberStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.deduct))
            } else {
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_rotate)
                binding.btnToggleMemberStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.color_green_status))
            }
            
            binding.btnToggleMemberStatus.setOnClickListener { onToggleStatus(item) }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<MemberItem>() {
        override fun areItemsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem == newItem
    }
}
