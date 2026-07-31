package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemStaffPromotionBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Promo

class StaffPromotionAdapter(
    private val onPromoClick: (Promo) -> Unit
) : ListAdapter<Promo, StaffPromotionAdapter.PromoViewHolder>(PromoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val binding = ItemStaffPromotionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromoViewHolder(binding, onPromoClick)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PromoViewHolder(private val binding: ItemStaffPromotionBinding, private val onPromoClick: (Promo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val tvStaffPromoTitle: TextView = binding.tvStaffPromoTitle
        private val tvStaffPromoDetail: TextView = binding.tvStaffPromoDetail
        private val tvStaffPromoCode: TextView = binding.tvStaffPromoCode
        private val imgPromoIcon = binding.imgPromoIcon

        fun bind(promo: Promo) {
            tvStaffPromoTitle.text = promo.title
            tvStaffPromoDetail.text = promo.detail ?: "No detailed description"
            tvStaffPromoCode.text = promo.discountCode

            com.bumptech.glide.Glide.with(itemView.context)
                .load(promo.imageUrl)
                .placeholder(R.drawable.bg_placeholder_card)
                .error(R.drawable.bg_placeholder_card)
                .into(imgPromoIcon)

            itemView.setOnClickListener {
                onPromoClick(promo)
            }
        }
    }

    class PromoDiffCallback : DiffUtil.ItemCallback<Promo>() {
        override fun areItemsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem == newItem
    }
}
