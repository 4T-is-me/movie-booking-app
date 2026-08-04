package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.ItemAdminMemberBinding

class MemberAdapter(
    private val onDetailClick: (MemberItem) -> Unit,
    private val onToggleStatus: (MemberItem) -> Unit
) : ListAdapter<MemberItem, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemAdminMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemAdminMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(member: MemberItem) {
            binding.tvMemberName.text = member.fullName ?: "Unknown"
            binding.tvMemberPoints.text = "${member.rewardPoints ?: 0}"

            // Status: 1 is Active, 0 is Locked/Inactive
            val isActive = member.account?.status == 1
            
            if (!isActive) {
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_view)
                binding.btnToggleMemberStatus.contentDescription = "Reactivate Member"
                binding.btnToggleMemberStatus.setColorFilter(binding.root.context.getColor(R.color.color_green_status))
            } else {
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_delete)
                binding.btnToggleMemberStatus.contentDescription = "Delete Member"
                binding.btnToggleMemberStatus.setColorFilter(binding.root.context.getColor(R.color.deduct))
            }

            binding.btnDetailMember.setOnClickListener { onDetailClick(member) }
            binding.btnToggleMemberStatus.setOnClickListener { onToggleStatus(member) }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<MemberItem>() {
        override fun areItemsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem == newItem
    }
}
