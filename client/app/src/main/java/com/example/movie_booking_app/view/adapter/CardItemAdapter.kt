package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.databinding.ItemCardPersonBinding

class CardItemAdapter : RecyclerView.Adapter<CardItemAdapter.DirectorViewHolder>(){

    private var list: List<String> = emptyList()

    fun submitList(newList: List<String>) {
        val diffCallback = StringListDiffCallback(this.list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }
    inner class DirectorViewHolder(val binding: ItemCardPersonBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectorViewHolder {
        val binding = ItemCardPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DirectorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DirectorViewHolder, position: Int) {
        val director = list[position]
        val nameParts = director.trim().split("\\s+".toRegex())
        holder.binding.txtFname.text = nameParts.last()
        holder.binding.txtLname.text =
            if (nameParts.size > 1)
                nameParts.dropLast(1).joinToString(" ")
            else
                ""
    }


    override fun getItemCount(): Int {
       return list.size
    }
}

class StringListDiffCallback(private val oldList: List<String>, private val newList: List<String>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}