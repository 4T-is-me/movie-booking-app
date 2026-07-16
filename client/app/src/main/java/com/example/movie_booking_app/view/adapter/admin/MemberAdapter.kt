package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.ItemAdminMemberBinding
import java.util.Locale

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
            binding.tvMemberName.text = item.fullName ?: binding.root.context.getString(R.string.user_name_placeholder)
            binding.tvMemberPoints.text = String.format(Locale.getDefault(), "%,d", item.rewardPoints ?: 0)
            
            val isActive = item.account?.status != 0
            
            // Load Avatar
            Glide.with(binding.ivMemberAvatar.context)
                .load(item.image)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivMemberAvatar)

            binding.btnDetailMember.setOnClickListener { onDetailClick(item) }
            binding.btnAdjustScore.setOnClickListener { onAdjustScore(item) }
            
            // If Active show trash icon (to Delete), if Inactive show Restore icon (to Reactivate)
            if (isActive) {
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_delete)
                binding.btnToggleMemberStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.deduct))
                binding.btnToggleMemberStatus.contentDescription = binding.root.context.getString(R.string.admin_delete)
            } else {
                // Use rotate/restore icon for Reactivate
                binding.btnToggleMemberStatus.setImageResource(android.R.drawable.ic_menu_rotate)
                binding.btnToggleMemberStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.color_green_status))
                binding.btnToggleMemberStatus.contentDescription = binding.root.context.getString(R.string.admin_reactivate)
            }
            
            binding.btnToggleMemberStatus.setOnClickListener { onToggleStatus(item) }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<MemberItem>() {
        override fun areItemsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MemberItem, newItem: MemberItem): Boolean = oldItem == newItem
    }
}
