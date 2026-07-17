package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.response.ManagerStaffItem
import com.example.movie_booking_app.databinding.ItemStaffSelectBinding

class ManagerStaffSelectAdapter(private val onSelectionChanged: (List<ManagerStaffItem>) -> Unit)
    : ListAdapter<ManagerStaffItem, ManagerStaffSelectAdapter.StaffSelectViewHolder>(StaffDiffCallback()){

    //use hashSet to save ID staff selected
    private val selectedStaffIds = HashSet<String>()
    private var isFirstLoad = true

    override fun submitList(list: List<ManagerStaffItem>?) {
        if (isFirstLoad && !list.isNullOrEmpty()) {
            // first load, default = select all
            list.forEach { staff ->
                staff.id?.let { selectedStaffIds.add(it) }
            }
            isFirstLoad = false
        }
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffSelectViewHolder {
        val binding = ItemStaffSelectBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return StaffSelectViewHolder(binding)
    }
    override fun onBindViewHolder(holder: StaffSelectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedStaff(): List<ManagerStaffItem> {
        return currentList.filter { selectedStaffIds.contains(it.id) }
    }

    inner class StaffSelectViewHolder(private val binding: ItemStaffSelectBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(item: ManagerStaffItem) {
                binding.tvStaffName.text = item.fullName ?: "Unknown"
                binding.tvStaffPosition.text = item.position ?: "Staff"
                //avoid infinite loop
                binding.cbSelect.setOnCheckedChangeListener(null)
                binding.cbSelect.isChecked = selectedStaffIds.contains(item.id)
                binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    item.id?.let { id ->
                        if (isChecked) {
                            selectedStaffIds.add(id)
                        } else {
                            selectedStaffIds.remove(id)
                        }
                        onSelectionChanged(getSelectedStaff())
                    }
                }
                binding.root.setOnClickListener {
                    binding.cbSelect.performClick()
                }
            }
        }

    class StaffDiffCallback : DiffUtil.ItemCallback<ManagerStaffItem>() {
        override fun areItemsTheSame(oldItem: ManagerStaffItem, newItem: ManagerStaffItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ManagerStaffItem, newItem: ManagerStaffItem): Boolean {
            return oldItem == newItem
        }
    }
}