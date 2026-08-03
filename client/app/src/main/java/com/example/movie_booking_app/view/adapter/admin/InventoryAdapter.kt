package com.example.movie_booking_app.view.adapter.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.Inventory
import com.example.movie_booking_app.databinding.ItemAdminInventoryBinding

class InventoryAdapter(
    private val onRefillClick: (Inventory) -> Unit
) : ListAdapter<Inventory, InventoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onRefillClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAdminInventoryBinding,
        private val onRefillClick: (Inventory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Inventory) {
            with(binding) {
                tvItemName.text = item.itemName
                tvItemCode.text = item.itemCode
                tvQuantity.text = item.quantity.toString()

                val isLowStock = item.quantity <= item.lowStockAlert
                tvLowStockWarning.isVisible = isLowStock
                
                if (isLowStock) {
                    tvQuantity.setTextColor(Color.RED)
                } else {
                    tvQuantity.setTextColor(Color.WHITE)
                }

                btnRefill.setOnClickListener { onRefillClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Inventory>() {
        override fun areItemsTheSame(oldItem: Inventory, newItem: Inventory): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Inventory, newItem: Inventory): Boolean = oldItem == newItem
    }
}
