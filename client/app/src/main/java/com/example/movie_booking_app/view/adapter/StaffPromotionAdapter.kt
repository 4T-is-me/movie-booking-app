package com.example.movie_booking_app.view.adapter

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff_promotion, parent, false)
        return PromoViewHolder(view, onPromoClick)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PromoViewHolder(
        view: View,
        private val onPromoClick: (Promo) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvStaffPromoTitle: TextView = view.findViewById(R.id.tvStaffPromoTitle)
        private val tvStaffPromoDetail: TextView = view.findViewById(R.id.tvStaffPromoDetail)
        private val tvStaffPromoCode: TextView = view.findViewById(R.id.tvStaffPromoCode)

        fun bind(promo: Promo) {
            tvStaffPromoTitle.text = promo.title
            tvStaffPromoDetail.text = promo.detail ?: "Không có mô tả chi tiết"
            tvStaffPromoCode.text = promo.discountCode

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
