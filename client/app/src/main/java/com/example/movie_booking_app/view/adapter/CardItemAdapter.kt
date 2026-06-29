package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.databinding.ItemCardPersonBinding

class CardItemAdapter(private val directors: List<String>) : RecyclerView.Adapter<CardItemAdapter.DirectorViewHolder>() {
    inner class DirectorViewHolder(val binding: ItemCardPersonBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectorViewHolder {
        val binding = ItemCardPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DirectorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DirectorViewHolder, position: Int) {
        val director = directors[position]
        val nameParts = director.trim().split("\\s+".toRegex())
        holder.binding.txtFname.text = nameParts.last()
        holder.binding.txtLname.text =
            if (nameParts.size > 1)
                nameParts.dropLast(1).joinToString(" ")
            else
                ""
    }


    override fun getItemCount(): Int {
       return directors.size
    }
}