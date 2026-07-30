package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.databinding.ItemAdminFoodBinding
import java.text.NumberFormat
import java.util.Locale

class AdminFoodAdapter(
    private val onEditClick: (FoodBeverage) -> Unit,
    private val onDeleteClick: (FoodBeverage) -> Unit
) : ListAdapter<FoodBeverage, AdminFoodAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAdminFoodBinding,
        private val onEditClick: (FoodBeverage) -> Unit,
        private val onDeleteClick: (FoodBeverage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodBeverage) {
            with(binding) {
                tvName.text = item.name
                tvCategory.text = item.category.replaceFirstChar { it.uppercase() }
                tvPrice.text = item.price.toVnd()

                if (!item.isActive) {
                    root.alpha = 0.5f
                    btnDelete.setImageResource(android.R.drawable.ic_menu_rotate)
                } else {
                    root.alpha = 1.0f
                    btnDelete.setImageResource(R.drawable.ic_delete)
                }

                val imageUrl = when {
                    item.imageUrl.isNullOrBlank() -> null
                    item.imageUrl.startsWith("http") -> item.imageUrl
                    else -> "${com.example.movie_booking_app.utils.Constants.BASE_URL.removeSuffix("/")}${item.imageUrl}"
                }

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imgProduct)

                btnEdit.setOnClickListener { onEditClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FoodBeverage>() {
        override fun areItemsTheSame(oldItem: FoodBeverage, newItem: FoodBeverage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodBeverage, newItem: FoodBeverage): Boolean = oldItem == newItem
    }
}

/**
 * Local extension for AdminFoodAdapter
 */
private fun Int.toVnd(): String {
    val formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMANY)
    return "${formatter.format(this)} VND"
}
