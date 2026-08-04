package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.databinding.ItemServicesBinding
import com.google.android.material.imageview.ShapeableImageView

class ServiceAdapter : ListAdapter<Service, ServiceAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ServiceViewHolder(private val binding: ItemServicesBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imgIcon: ShapeableImageView = binding.imgServiceIcon
        private val tvName: TextView = binding.tvServiceName

        fun bind(service: Service) {
            tvName.text = service.name
            
            val imageUrl = when {
                service.name.contains("4DX", ignoreCase = true) -> "https://res.cloudinary.com/fmu581pj/image/upload/v1783842048/4dx_xjfljb.webp"
                service.name.contains("IMAX", ignoreCase = true) -> "https://res.cloudinary.com/fmu581pj/image/upload/v1783842047/imax_jhxh12.jpg"
                else -> service.iconUrl
            }

            if (imageUrl.isNotEmpty()) {
                imgIcon.clearColorFilter()
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_info_details)
                    .error(android.R.drawable.ic_menu_info_details)
                    .into(imgIcon)
            } else {
                imgIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                imgIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.white))
            }
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Service>() {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem == newItem
    }
}
