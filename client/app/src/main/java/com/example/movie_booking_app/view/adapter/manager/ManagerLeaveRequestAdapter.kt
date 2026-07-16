package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestResponse
import com.example.movie_booking_app.databinding.ItemLeaveRequestBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.NameFormatter
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class ManagerLeaveRequestAdapter(private val listener: LeaveRequestListener) :
    RecyclerView.Adapter<ManagerLeaveRequestAdapter.ManagerLeaveViewHolder>() {

    interface LeaveRequestListener {
        fun onApprove(item: ManagerLeaveRequestResponse)
        fun onReject(item: ManagerLeaveRequestResponse)
        fun onUndo(item: ManagerLeaveRequestResponse)
    }

    private var items: List<ManagerLeaveRequestResponse> = emptyList()
    private val actionStates = mutableMapOf<String, String>()

    fun updateData(newItems: List<ManagerLeaveRequestResponse>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManagerLeaveViewHolder {
        val binding = ItemLeaveRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ManagerLeaveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManagerLeaveViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private fun getShiftTimeDisplay(shift: String): String {
        return when (shift) {
            "Morning" -> Constants.MORNING
            "Afternoon" -> Constants.AFTERNOON
            "Evening" -> Constants.EVENING
            "Night" -> Constants.NIGHT
            else -> Constants.FULLTIME
        }
    }

    inner class ManagerLeaveViewHolder(private val binding: ItemLeaveRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ManagerLeaveRequestResponse) {
            val context = binding.root.context
            val cardView = binding.root as? MaterialCardView

            val currentState = actionStates[item.leaveRequestId] ?: item.status

            val initials = NameFormatter.getAvatarInitials(item.staffName)
            val shiftTime = getShiftTimeDisplay(item.shift)
            binding.tvAvatarText.text = if (initials.isNotEmpty()) initials else "ST"
            binding.tvStaffName.text = item.staffName
            binding.tvPosition.text = item.staffPosition
            binding.tvShiftInfo.text = "${item.shift} - ${shiftTime}"
            binding.tvWorkDate.text = item.workDate
            binding.tvReason.text = "\"${item.reason}\""

            // --- Status UI / Stroke Card / Undo Bar ---
            when (currentState.lowercase(Locale.getDefault())) {
                "pending" -> {
                    binding.tvStatusBadge.text = context.getString(R.string.status_pending)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_gold_orange))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gold)

                    binding.layoutButtons.visibility = View.VISIBLE
                    binding.layoutUndoBar.visibility = View.GONE

                    cardView?.strokeWidth = 1
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_stroke)
                }
                "approved" -> {
                    binding.tvStatusBadge.text = context.getString(R.string.status_approved)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_success_green))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)

                    binding.layoutButtons.visibility = View.GONE

                    cardView?.strokeWidth = 4
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_success_green)

                    if (actionStates.containsKey(item.leaveRequestId)) {
                        binding.layoutUndoBar.visibility = View.VISIBLE
                        binding.tvUndoMessage.text = "Request approved"
                        binding.tvUndoMessage.setTextColor(ContextCompat.getColor(context, R.color.color_success_green))
                        binding.layoutUndoBar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_undo_approved))
                    } else {
                        binding.layoutUndoBar.visibility = View.GONE
                    }
                }
                "rejected" -> {
                    binding.tvStatusBadge.text = context.getString(R.string.status_rejected)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)

                    binding.layoutButtons.visibility = View.GONE

                    cardView?.strokeWidth = 4
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_error_red)

                    if (actionStates.containsKey(item.leaveRequestId)) {
                        binding.layoutUndoBar.visibility = View.VISIBLE
                        binding.tvUndoMessage.text = "Request rejected"
                        binding.tvUndoMessage.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
                        binding.layoutUndoBar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_undo_rejected))
                    } else {
                        binding.layoutUndoBar.visibility = View.GONE
                    }
                }
            }

            // --- Event Listeners ---
            binding.btnApprove.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    actionStates[item.leaveRequestId] = "Approved"
                    notifyItemChanged(currentPosition)
                    listener.onApprove(item)
                }
            }

            binding.btnReject.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    actionStates[item.leaveRequestId] = "Rejected"
                    notifyItemChanged(currentPosition)
                    listener.onReject(item)
                }
            }

            binding.btnUndo.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    actionStates.remove(item.leaveRequestId)
                    notifyItemChanged(currentPosition)
                    listener.onUndo(item)
                }
            }
        }
    }
}