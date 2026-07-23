package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemPromoBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Promo

class PromoAdapter : ListAdapter<Promo, PromoAdapter.PromoViewHolder>(PromoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val binding = ItemPromoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PromoViewHolder(private val binding: ItemPromoBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imgPromo: ImageView = binding.imgPromo

        fun bind(promo: Promo) {
            Glide.with(itemView.context)
                .load(promo.imageUrl)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPromo)
        }
    }

    class PromoDiffCallback : DiffUtil.ItemCallback<Promo>() {
        override fun areItemsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem == newItem
    }
}
