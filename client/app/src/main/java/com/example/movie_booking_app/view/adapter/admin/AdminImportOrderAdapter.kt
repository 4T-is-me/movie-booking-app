package com.example.movie_booking_app.view.adapter.admin


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ImportOrder
import com.example.movie_booking_app.databinding.ItemAdminImportOrderBinding
import com.example.movie_booking_app.utils.Constants
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
            val context = itemView.context

            with(binding) {
                tvImportCode.text = order.importCode
                tvSupplierName.text = order.supplierId?.name ?: Constants.UNKNOWN
                tvTotalAmount.text = currencyFormat.format(order.totalAmount)
                tvItemCount.text = binding.root.context.getString(R.string.admin_import_item_count, order.items.size)
                tvCreatedAt.text = order.createdAt?.take(10) ?: ""
                tvNotes.text = order.notes?.takeIf { it.isNotEmpty() } ?: binding.root.context.getString(R.string.admin_import_no_notes)

                val itemsSummary = order.items.joinToString(", ") { binding.root.context.getString(R.string.admin_import_item_format, it.itemName, it.quantity) }
                tvItemsList.text = binding.root.context.getString(R.string.admin_import_items_list, itemsSummary)

                // Status display
                val (statusText, statusColor) = when (order.status) {
                    Constants.ImportOrderStatus.PENDING -> Pair(
                        Constants.ImportOrderStatus.DISPLAY_PENDING,
                        ContextCompat.getColor(context, R.color.status_pending)
                    )
                    Constants.ImportOrderStatus.APPROVED -> Pair(
                        Constants.ImportOrderStatus.DISPLAY_APPROVED,
                        ContextCompat.getColor(context, R.color.status_approved)
                    )
                    Constants.ImportOrderStatus.REJECTED -> Pair(
                        Constants.ImportOrderStatus.DISPLAY_REJECTED,
                        ContextCompat.getColor(context, R.color.status_rejected)
                    )
                    Constants.ImportOrderStatus.CANCELLED -> Pair(
                        Constants.ImportOrderStatus.DISPLAY_CANCELLED,
                        ContextCompat.getColor(context, R.color.status_cancelled)
                    )
                    else -> Pair(
                        order.status,
                        ContextCompat.getColor(context, R.color.status_default)
                    )
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(statusColor)

                // Show Approve/Reject only for PENDING
                val isPending = order.status == Constants.ImportOrderStatus.PENDING
                btnApprove.visibility = if (isPending) View.VISIBLE else View.GONE
                btnReject.visibility  = if (isPending) View.VISIBLE else View.GONE

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