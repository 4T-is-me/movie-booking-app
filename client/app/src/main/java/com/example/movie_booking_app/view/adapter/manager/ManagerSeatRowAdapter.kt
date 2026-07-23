package com.example.movie_booking_app.view.adapter.manager

import com.example.movie_booking_app.databinding.ItemManagerSeatRowBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerSeatRow

class ManagerSeatRowAdapter(
    private var rows: List<ManagerSeatRow> = emptyList()
) : RecyclerView.Adapter<ManagerSeatRowAdapter.RowViewHolder>() {

    fun updateData(newRows: List<ManagerSeatRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemManagerSeatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = rows[position]
        holder.bind(row)
    }

    override fun getItemCount(): Int = rows.size

    class RowViewHolder(private val binding: ItemManagerSeatRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvRowName: TextView = binding.tvRowName
        private val rvSeats: RecyclerView = binding.rvSeats

        fun bind(row: ManagerSeatRow) {
            tvRowName.text = row.row
            rvSeats.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            rvSeats.adapter = ManagerSeatAdapter(row.seats ?: emptyList())
        }
    }
}
