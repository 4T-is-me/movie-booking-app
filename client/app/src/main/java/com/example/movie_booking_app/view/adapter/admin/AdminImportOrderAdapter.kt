package com.example.movie_booking_app.view.adapter.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.databinding.ItemAdminImportOrderBinding
import java.text.NumberFormat
import java.util.Locale

class AdminImportOrderAdapter(
    private val onApproveClick: (ImportOrder) -> Unit,
    private val onRejectClick: (ImportOrder) -> Unit
) : ListAdapter<ImportOrder, AdminImportOrderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminImportOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onApproveClick, onRejectClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAdminImportOrderBinding,
        private val onApproveClick: (ImportOrder) -> Unit,
        private val onRejectClick: (ImportOrder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        fun bind(order: ImportOrder) {
            with(binding) {
                tvImportCode.text = order.importCode
                tvSupplierName.text = order.supplierId?.name ?: "Unknown"
                tvTotalAmount.text = currencyFormat.format(order.totalAmount)
                tvItemCount.text = "${order.items.size} items"
                tvCreatedAt.text = order.createdAt?.take(10) ?: ""
                tvNotes.text = order.notes?.takeIf { it.isNotEmpty() } ?: "No notes"

                // Status display
                val (statusText, statusColor) = when (order.status) {
                    "PENDING"   -> Pair("Pending",  Color.parseColor("#FFA500"))
                    "APPROVED"  -> Pair("Approved",   Color.parseColor("#4CAF50"))
                    "REJECTED"  -> Pair("Rejected", Color.parseColor("#F44336"))
                    "CANCELLED" -> Pair("Cancelled",     Color.parseColor("#9E9E9E"))
                    else        -> Pair(order.status,  Color.GRAY)
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(statusColor)

                // Show Approve/Reject only for PENDING
                val isPending = order.status == "PENDING"
                btnApprove.visibility = if (isPending) android.view.View.VISIBLE else android.view.View.GONE
                btnReject.visibility  = if (isPending) android.view.View.VISIBLE else android.view.View.GONE

                btnApprove.setOnClickListener { onApproveClick(order) }
                btnReject.setOnClickListener  { onRejectClick(order) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImportOrder>() {
        override fun areItemsTheSame(oldItem: ImportOrder, newItem: ImportOrder) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ImportOrder, newItem: ImportOrder) = oldItem == newItem
    }
}
