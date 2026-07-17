package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.databinding.ItemShiftDraftPreviewBinding
import com.example.movie_booking_app.utils.StaffShiftDraft

class ManagerShiftDraftPreviewAdapter(private var items: List<StaffShiftDraft> = emptyList()): RecyclerView.Adapter<ManagerShiftDraftPreviewAdapter.ViewHolder>() {

    fun updateData(newItems: List<StaffShiftDraft>) {
        items = newItems
        notifyDataSetChanged()
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