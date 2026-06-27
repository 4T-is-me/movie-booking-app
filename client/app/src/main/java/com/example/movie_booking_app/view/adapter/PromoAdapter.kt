package com.example.movie_booking_app.view.adapter

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
import com.example.movie_booking_app.data.model.ui.Promo

class PromoAdapter : ListAdapter<Promo, PromoAdapter.PromoViewHolder>(PromoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promo, parent, false)
        return PromoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PromoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgPromo: ImageView = view.findViewById(R.id.imgPromo)

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
