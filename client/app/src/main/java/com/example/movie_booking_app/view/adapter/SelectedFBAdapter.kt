package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.ComboDetail
import com.example.movie_booking_app.databinding.ItemPaymentFbSummaryBinding
import java.text.NumberFormat
import java.util.Locale

class SelectedFBAdapter : ListAdapter<ComboDetail, SelectedFBAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentFbSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemPaymentFbSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ComboDetail) {
            binding.tvItemName.text = "${item.name} x${item.quantity}"
            binding.tvItemPrice.text = formatCurrency(item.price * item.quantity)
        }

        private fun formatCurrency(amount: Int): String {
            val formatter = NumberFormat.getInstance(Locale.GERMANY)
            return "${formatter.format(amount)} VND"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ComboDetail>() {
        override fun areItemsTheSame(oldItem: ComboDetail, newItem: ComboDetail): Boolean = oldItem.comboId == newItem.comboId
        override fun areContentsTheSame(oldItem: ComboDetail, newItem: ComboDetail): Boolean = oldItem == newItem
    }
}
