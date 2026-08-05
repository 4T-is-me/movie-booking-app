package com.example.movie_booking_app.view.adapter.manager

import android.graphics.Color
import androidx.core.content.ContextCompat
import com.example.movie_booking_app.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.databinding.ItemImportOrderBinding
import java.text.NumberFormat
import java.util.Locale

class ImportOrderAdapter(
    private val onCancelClick: (ImportOrder) -> Unit
) : ListAdapter<ImportOrder, ImportOrderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImportOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onCancelClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemImportOrderBinding,
        private val onCancelClick: (ImportOrder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        fun bind(order: ImportOrder) {
            with(binding) {
                tvImportCode.text = order.importCode
                tvSupplierName.text = order.supplierId?.name ?: binding.root.context.getString(R.string.import_order_unknown_supplier)
                tvTotalAmount.text = currencyFormat.format(order.totalAmount)
                tvItemCount.text = binding.root.context.getString(R.string.admin_import_item_count, order.items.size)
                tvCreatedAt.text = order.createdAt?.take(10) ?: ""
                tvNotes.text = order.notes?.takeIf { it.isNotEmpty() } ?: binding.root.context.getString(R.string.admin_import_no_notes)

                val itemsSummary = order.items.joinToString(", ") { binding.root.context.getString(R.string.admin_import_item_format, it.itemName, it.quantity) }
                tvItemsList.text = binding.root.context.getString(R.string.admin_import_items_list, itemsSummary)

                // Status chip
                val (statusText, statusColor) = when (order.status) {
                    "PENDING"   -> Pair("Pending",  ContextCompat.getColor(binding.root.context, R.color.status_pending))
                    "APPROVED"  -> Pair("Approved",   ContextCompat.getColor(binding.root.context, R.color.status_approved))
                    "REJECTED"  -> Pair("Rejected", ContextCompat.getColor(binding.root.context, R.color.status_rejected))
                    "CANCELLED" -> Pair("Cancelled",     ContextCompat.getColor(binding.root.context, R.color.status_cancelled))
                    else        -> Pair(order.status,  Color.GRAY)
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(statusColor)

                // Show cancel button only for PENDING orders
                btnCancel.visibility = if (order.status == "PENDING")
                    View.VISIBLE else View.GONE
                btnCancel.setOnClickListener { onCancelClick(order) }

                // Show rejection reason if rejected
                val hasRejectionReason = !order.rejectionReason.isNullOrEmpty()
                tvRejectionLabel.visibility = if (order.status == "REJECTED" && hasRejectionReason)
                    View.VISIBLE else View.GONE
                tvRejectionReason.visibility = tvRejectionLabel.visibility
                tvRejectionReason.text = order.rejectionReason ?: ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ImportOrder>() {
        override fun areItemsTheSame(oldItem: ImportOrder, newItem: ImportOrder) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ImportOrder, newItem: ImportOrder) = oldItem == newItem
    }
}
