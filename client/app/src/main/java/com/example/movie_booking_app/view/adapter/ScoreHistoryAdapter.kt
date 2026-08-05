package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ScoreHistory
import com.example.movie_booking_app.databinding.ItemScoreHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ScoreHistoryAdapter(private var list: List<ScoreHistory> = emptyList()) : RecyclerView.Adapter<ScoreHistoryAdapter.ScoreHistoryViewHolder>() {
    inner class ScoreHistoryViewHolder(val binding: ItemScoreHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreHistoryViewHolder {
        val binding = ItemScoreHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreHistoryViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            tvHistoryTitle.text = item.reason

            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
                // Display format: day/month/year - hour:minute
                val formatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                val date = parser.parse(item.createdAt ?: "")
                tvHistoryDate.text = if (date != null) formatter.format(date) else "Recent"
            } catch (e: Exception) {
                tvHistoryDate.text = holder.itemView.context.getString(R.string.auto_str_recent)
            }

            val textPlus = ContextCompat.getColor(holder.itemView.context, R.color.textPlus)
            val backgroundPlus = ContextCompat.getColor(holder.itemView.context, R.color.backgroundPlus)
            val iconPlus = ContextCompat.getColor(holder.itemView.context, R.color.iconPlus)
            val deduct = ContextCompat.getColor(holder.itemView.context, R.color.deduct)
            val backgroundDeduct = ContextCompat.getColor(holder.itemView.context, R.color.backgroundDeduct)
            val iconDeduct = ContextCompat.getColor(holder.itemView.context, R.color.iconDeduct)

            val isDeduct = item.usedPoints > 0 || item.reason == "USE_SCORE"

            if (isDeduct) {
                tvPointsValue.text = holder.itemView.context.getString(R.string.used_points_format, item.usedPoints)
                tvPointsValue.setTextColor(deduct)
                iconContainer.setCardBackgroundColor(backgroundDeduct)
                ivHistoryIcon.setImageResource(R.drawable.ic_remove)
                ivHistoryIcon.setColorFilter(iconDeduct)
            } else {
                tvPointsValue.text = holder.itemView.context.getString(R.string.earned_points_format, item.earnedPoints)
                tvPointsValue.setTextColor(textPlus)
                iconContainer.setCardBackgroundColor(backgroundPlus)
                ivHistoryIcon.setImageResource(R.drawable.ic_add)
                ivHistoryIcon.setColorFilter(iconPlus)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ScoreHistory>) {
        val diffCallback = ScoreHistoryDiffCallback(this.list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }
}
class ScoreHistoryDiffCallback(private val oldList: List<ScoreHistory>, private val newList: List<ScoreHistory>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]._id == newList[newItemPosition]._id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}