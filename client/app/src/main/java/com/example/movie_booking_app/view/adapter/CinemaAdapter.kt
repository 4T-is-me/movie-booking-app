package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.databinding.ItemCinemaBinding
import com.example.movie_booking_app.utils.Constants

class CinemaAdapter(private val cinemas: List<Cinema>, private val onCinemaClick: (Cinema?) -> Unit):  RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder>() {
    private var selectedPosition = RecyclerView.NO_POSITION

    inner class CinemaViewHolder(val binding: ItemCinemaBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
       val binding = ItemCinemaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CinemaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        val cinema = cinemas[position]
        holder.binding.txtCinemaName.text = cinema.cinemaName
        holder.binding.txtCinemaAddress.text = "${cinema.address}, ${cinema.city}"
        //image logo
        val imageUrl = cinema.image?.let {
            if (it.startsWith("http")) {
                it
            } else {
                "${Constants.BASE_URL}${it.removePrefix("/")}"
            }
        }

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.stat_notify_error)
            .into(holder.binding.imgCinemaLogo)
        // Highlight selected cinema
        if (position == selectedPosition) {
            holder.binding.root.setCardBackgroundColor("#2D2208".toColorInt())
            holder.binding.root.strokeColor = "#D9A300".toColorInt()
            holder.binding.root.strokeWidth = 4
        } else {
            holder.binding.root.setCardBackgroundColor("#1A1A1A".toColorInt())
            holder.binding.root.strokeColor = "#454545".toColorInt()
            holder.binding.root.strokeWidth = 1
        }

        holder.binding.root.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            // Click the selected item again -> deselect it
            if (currentPosition == selectedPosition) {
                val oldPosition = selectedPosition
                selectedPosition = RecyclerView.NO_POSITION
                notifyItemChanged(oldPosition)

                // notify the outside that the selection has been cleared
                onCinemaClick(null)  // or onCinemaClick(null) if callback allows
                return@setOnClickListener
            }
            // select new item
            val oldPosition = selectedPosition
            selectedPosition = currentPosition

            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }
            notifyItemChanged(selectedPosition)
            onCinemaClick(cinema)
        }
    }

    override fun getItemCount(): Int {
        return cinemas.size
    }
}