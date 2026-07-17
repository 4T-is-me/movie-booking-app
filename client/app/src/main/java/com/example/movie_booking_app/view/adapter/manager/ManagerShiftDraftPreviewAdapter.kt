package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.databinding.ItemShiftDraftPreviewBinding
import com.example.movie_booking_app.utils.StaffShiftDraft

class ManagerShiftDraftPreviewAdapter(private var items: List<StaffShiftDraft> = emptyList()): RecyclerView.Adapter<ManagerShiftDraftPreviewAdapter.ViewHolder>() {

    fun updateData(newItems: List<StaffShiftDraft>) {
        val diffCallback = ShiftDraftDiffCallback(this.items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShiftDraftPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemShiftDraftPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StaffShiftDraft) {
            binding.tvStaffName.text = item.staffName
            binding.tvShiftBadge.text = item.shift
            binding.tvWorkDate.text = item.workDate

            if (item.isHoliday) {
                binding.tvHolidayBadge.visibility = View.VISIBLE
            } else {
                binding.tvHolidayBadge.visibility = View.GONE
            }
        }
    }
}

class ShiftDraftDiffCallback(private val oldList: List<StaffShiftDraft>, private val newList: List<StaffShiftDraft>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.staffName == newItem.staffName
                && oldItem.workDate == newItem.workDate
                && oldItem.shift == newItem.shift
                && oldItem.staffId == newItem.staffId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // So sánh sâu toàn bộ nội dung (bao gồm cả trạng thái xem có đổi thành ngày lễ isHoliday không)
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}