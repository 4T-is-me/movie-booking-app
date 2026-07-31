package com.example.movie_booking_app.view.adapter.manager

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.api.ImportOrderItemRequest
import com.example.movie_booking_app.databinding.ItemImportOrderInputBinding

/**
 * Adapter for entering import order items (quantity + purchase price).
 * Supports mutable list with add/remove and live total computation.
 */
class ImportOrderItemInputAdapter(
    private val onTotalChanged: (Long) -> Unit
) : ListAdapter<ImportOrderItemRequest, ImportOrderItemInputAdapter.ViewHolder>(DiffCallback()) {

    private val mutableItems = mutableListOf<ImportOrderItemRequest>()

    private fun calculateTotal(): Long {
        return mutableItems.sumOf { it.quantity.toLong() * it.purchasePrice }
    }

    fun addItem(item: ImportOrderItemRequest) {
        mutableItems.add(item)
        submitList(mutableItems.toList())
        onTotalChanged(calculateTotal())
    }

    fun removeItem(position: Int) {
        if (position in mutableItems.indices) {
            mutableItems.removeAt(position)
            submitList(mutableItems.toList())
            onTotalChanged(calculateTotal())
        }
    }

    private fun updateItem(position: Int, quantity: Int, purchasePrice: Long) {
        if (position in mutableItems.indices) {
            mutableItems[position] = mutableItems[position].copy(
                quantity = quantity,
                purchasePrice = purchasePrice
            )
            submitList(mutableItems.toList())
            onTotalChanged(calculateTotal())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImportOrderInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ItemImportOrderInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var quantityWatcher: TextWatcher? = null
        private var priceWatcher: TextWatcher? = null

        fun bind(item: ImportOrderItemRequest, position: Int) {
            with(binding) {
                tvItemName.text = item.itemName
                tvItemCode.text = item.itemCode

                // Remove old watchers before setting text
                quantityWatcher?.let { etQuantity.removeTextChangedListener(it) }
                priceWatcher?.let { etPurchasePrice.removeTextChangedListener(it) }

                val qtyStr = if (item.quantity > 0) item.quantity.toString() else ""
                if (etQuantity.text.toString() != qtyStr) {
                    etQuantity.setText(qtyStr)
                    etQuantity.setSelection(qtyStr.length)
                }

                val priceStr = if (item.purchasePrice > 0) item.purchasePrice.toString() else ""
                if (etPurchasePrice.text.toString() != priceStr) {
                    etPurchasePrice.setText(priceStr)
                    etPurchasePrice.setSelection(priceStr.length)
                }

                // Re-add watchers
                quantityWatcher = createWatcher(position)
                priceWatcher = createWatcher(position)
                etQuantity.addTextChangedListener(quantityWatcher)
                etPurchasePrice.addTextChangedListener(priceWatcher)

                btnRemove.setOnClickListener { removeItem(position) }
            }
        }

        private fun createWatcher(position: Int) = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val qty = binding.etQuantity.text.toString().toIntOrNull() ?: 0
                val price = binding.etPurchasePrice.text.toString().toLongOrNull() ?: 0L
                updateItem(position, qty, price)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImportOrderItemRequest>() {
        override fun areItemsTheSame(oldItem: ImportOrderItemRequest, newItem: ImportOrderItemRequest) =
            oldItem.itemCode == newItem.itemCode
        override fun areContentsTheSame(oldItem: ImportOrderItemRequest, newItem: ImportOrderItemRequest) =
            oldItem == newItem
    }
}
