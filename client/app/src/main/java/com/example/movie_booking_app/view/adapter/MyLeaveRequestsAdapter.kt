package com.example.movie_booking_app.view.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.LeaveRequestResponse
import com.example.movie_booking_app.databinding.ItemMyLeaveRequestBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.DateUtils

class MyLeaveRequestsAdapter(
    private var list: List<LeaveRequestResponse> = emptyList(),
    private val onEditClick: (LeaveRequestResponse) -> Unit,
    private val onDeleteClick: (LeaveRequestResponse) -> Unit
) : RecyclerView.Adapter<MyLeaveRequestsAdapter.ViewHolder>(){

    fun submitList(newList: List<LeaveRequestResponse>) {
        val diffCallback = LeaveRequestDiffCallback(this.list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(val binding: ItemMyLeaveRequestBinding) : RecyclerView.ViewHolder(binding.root) {

        private val context = itemView.context

        fun bind(item: LeaveRequestResponse) {
            binding.tvLeaveDate.text = DateUtils.formatMovieDate(item.workDate)
            binding.tvLeaveShift.text = "${item.shift}"
            binding.tvLeaveReason.text = item.reason ?: context.getString(R.string.leave_no_reason)

            when (item.status) {
                Constants.LeaveStatus.PENDING -> {
                    val textColor = ContextCompat.getColor(context, R.color.color_gold)
                    val bgColor = ContextCompat.getColor(context, R.color.bg_pending_badge)

                    setDynamicBadge(binding.tvLeaveStatusBadge, context.getString(R.string.status_pending), textColor, bgColor)

                    binding.vStatusIndicator.setBackgroundColor(textColor)

                    binding.layoutRejectReason.visibility = View.GONE
                    binding.layoutPendingActions.visibility = View.VISIBLE

                    binding.btnEditLeave.setOnClickListener { onEditClick(item) }
                    binding.btnDeleteLeave.setOnClickListener { onDeleteClick(item) }
                }

                Constants.LeaveStatus.APPROVED -> {
                    val textColor = ContextCompat.getColor(context, R.color.color_green)
                    val bgColor = ContextCompat.getColor(context, R.color.bg_approved_badge)

                    setDynamicBadge(binding.tvLeaveStatusBadge, context.getString(R.string.status_approved), textColor, bgColor)

                    binding.vStatusIndicator.setBackgroundColor(textColor)

                    binding.layoutRejectReason.visibility = View.GONE
                    binding.layoutPendingActions.visibility = View.GONE
                }

                Constants.LeaveStatus.REJECTED -> {
                    val textColor = ContextCompat.getColor(context, R.color.color_red)
                    val bgColor = ContextCompat.getColor(context, R.color.bg_rejected_badge)

                    setDynamicBadge(binding.tvLeaveStatusBadge, context.getString(R.string.status_rejected), textColor, bgColor)

                    // Update left vertical indicator color for Rejected status
                    binding.vStatusIndicator.setBackgroundColor(textColor)

                    binding.layoutPendingActions.visibility = View.GONE
                    binding.layoutRejectReason.visibility = View.VISIBLE
                    binding.tvRejectReason.text = item.rejectReason ?: context.getString(R.string.leave_no_feedback)
                    binding.tvRejectReason.setTextColor(textColor)
                }

                else -> {
                    val textColor = ContextCompat.getColor(context, R.color.white)
                    val bgColor = ContextCompat.getColor(context, R.color.auto_color_94A3B8)

                    setDynamicBadge(binding.tvLeaveStatusBadge, item.status ?: context.getString(R.string.status_unknown), textColor, bgColor)
                    binding.vStatusIndicator.setBackgroundColor(textColor)

                    binding.layoutPendingActions.visibility = View.GONE
                    binding.layoutRejectReason.visibility = View.GONE
                }
            }
        }

        private fun setDynamicBadge(tv: TextView, text: String, textColor: Int, bgColor: Int) {
            tv.text = text
            tv.setTextColor(textColor)
            val shape = GradientDrawable().apply {
                cornerRadius = 8f * tv.resources.displayMetrics.density
                setColor(bgColor)
            }
            tv.background = shape
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyLeaveRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    private class LeaveRequestDiffCallback(
        private val oldList: List<LeaveRequestResponse>,
        private val newList: List<LeaveRequestResponse>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].leaveRequestId == newList[newItemPosition].leaveRequestId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}