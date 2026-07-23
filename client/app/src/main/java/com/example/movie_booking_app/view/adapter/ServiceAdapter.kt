package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemServicesBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Service
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
            // Placeholder logic for service icons
            imgIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            imgIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.white))
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Service>() {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean = oldItem == newItem
    }
}
