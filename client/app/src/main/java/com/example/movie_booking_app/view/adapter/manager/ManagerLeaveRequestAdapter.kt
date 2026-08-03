package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestResponse
import com.example.movie_booking_app.databinding.ItemLeaveRequestBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.NameFormatter
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
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
        // Clear actionStates for items whose server status is no longer Pending.
        // This prevents stale local states from being applied to wrong ViewHolders
        // during DiffUtil dispatch animations (position shift issue).
        newItems.forEach { item ->
            if (item.status != "Pending") {
                actionStates.remove(item.leaveRequestId)
            }
        }

        val diffCallback = LeaveRequestDiffCallback(this.items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
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
            Constants.SHIFTS[0]-> Constants.MORNING
            Constants.SHIFTS[1] -> Constants.AFTERNOON
            Constants.SHIFTS[2] -> Constants.EVENING
            Constants.SHIFTS[3] -> Constants.NIGHT
            else -> Constants.FULLTIME
        }
    }

    inner class ManagerLeaveViewHolder(private val binding: ItemLeaveRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ManagerLeaveRequestResponse) {
            val context = binding.root.context
            val cardView = binding.root as? MaterialCardView

            // Use the temporary state if available; otherwise, use the standard status from the server.
            val currentState = actionStates[item.leaveRequestId] ?: item.status

            val initials = NameFormatter.getAvatarInitials(item.staffName)
            val shiftTime = getShiftTimeDisplay(item.shift)

            binding.tvAvatarText.text = if (initials.isNotEmpty()) initials else context.getString(R.string.default_avatar_initials)
            binding.tvStaffName.text = item.staffName
            binding.tvPosition.text = item.staffPosition
            binding.tvShiftInfo.text = context.getString(R.string.format_shift_info, item.shift, shiftTime)
            binding.tvWorkDate.text = item.workDate
            binding.tvReason.text = context.getString(R.string.format_reason, item.reason)

            // --- Status UI / Stroke Card / Undo Bar ---
            when (currentState.lowercase(Locale.getDefault())) {
                Constants.STATUS_REQUEST[0] -> { // Pending
                    binding.tvStatusBadge.text = context.getString(R.string.status_pending)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_gold_orange))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_gold)

                    binding.layoutButtons.visibility = View.VISIBLE
                    binding.layoutUndoBar.visibility = View.GONE

                    cardView?.strokeWidth = 1
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_stroke)
                }

                Constants.STATUS_REQUEST[1] -> { // Approved
                    binding.tvStatusBadge.text = context.getString(R.string.status_approved)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_success_green))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)

                    binding.layoutButtons.visibility = View.GONE

                    // CHECK: Already claimed OR shift has expired.
                    if (item.isShiftTaken == true || isShiftExpired(item.workDate)) {
                        binding.layoutUndoBar.visibility = View.GONE
                    } else {
                        binding.layoutUndoBar.visibility = View.VISIBLE
                        binding.btnUndo.visibility = View.VISIBLE
                        binding.tvUndoMessage.text = context.getString(R.string.undo_msg_approved)
                        binding.tvUndoMessage.setTextColor(ContextCompat.getColor(context, R.color.color_success_green))
                        binding.layoutUndoBar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_undo_approved))
                    }

                    cardView?.strokeWidth = 4
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_success_green)
                }

                Constants.STATUS_REQUEST[2] -> { // Rejected
                    binding.tvStatusBadge.text = context.getString(R.string.status_rejected)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)

                    binding.layoutButtons.visibility = View.GONE

                    // CHECK: rejected case has exceeded the processing time limit
                    if (isShiftExpired(item.workDate)) {
                        binding.layoutUndoBar.visibility = View.GONE
                    } else {
                        binding.layoutUndoBar.visibility = View.VISIBLE
                        binding.btnUndo.visibility = View.VISIBLE
                        binding.tvUndoMessage.text = context.getString(R.string.undo_msg_rejected)
                        binding.tvUndoMessage.setTextColor(ContextCompat.getColor(context, R.color.color_error_red))
                        binding.layoutUndoBar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_undo_rejected))
                    }

                    cardView?.strokeWidth = 4
                    cardView?.strokeColor = ContextCompat.getColor(context, R.color.color_error_red)
                }
            }

            // --- Event Listeners ---
            binding.btnApprove.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    actionStates[item.leaveRequestId] = Constants.STATUS_REQUEST[1]
                    listener.onApprove(item)
                }
            }

            binding.btnReject.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onReject(item)
                }
            }

            binding.btnUndo.setOnClickListener {
                actionStates.remove(item.leaveRequestId)
                listener.onUndo(item)
            }
        }
    }
    fun setLocalRejectState(leaveRequestId: String) {
        actionStates[leaveRequestId] = Constants.STATUS_REQUEST[2]
        val position = items.indexOfFirst { it.leaveRequestId == leaveRequestId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    private fun isShiftExpired(workDateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val workDate = sdf.parse(workDateStr) ?: return false

            val today = Date()

            workDate.before(today)
        } catch (e: Exception) {
            false
        }
    }
}


class LeaveRequestDiffCallback(private val oldList: List<ManagerLeaveRequestResponse>, private val newList: List<ManagerLeaveRequestResponse>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].leaveRequestId == newList[newItemPosition].leaveRequestId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}