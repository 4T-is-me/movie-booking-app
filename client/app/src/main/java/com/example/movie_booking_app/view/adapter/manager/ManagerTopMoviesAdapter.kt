package com.example.movie_booking_app.view.adapter.manager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.TopMoviesResponse
import com.example.movie_booking_app.databinding.ItemTopMovieBinding

class ManagerTopMoviesAdapter : RecyclerView.Adapter<ManagerTopMoviesAdapter.TopMoviesViewHolder>() {
    private var moviesList: List<TopMoviesResponse> = emptyList()
    private var maxTickets = 1
    fun submitList(newList: List<TopMoviesResponse>?) {
        val validatedNewList = newList.orEmpty()

        val diffCallback = TopMoviesDiffCallback(this.moviesList, validatedNewList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.moviesList = validatedNewList
        this.maxTickets = if (moviesList.isNotEmpty()) moviesList[0].totalTickets.coerceAtLeast(1) else 1

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopMoviesViewHolder {
        val binding = ItemTopMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopMoviesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopMoviesViewHolder, position: Int) {
        val item = moviesList[position]
        val rank = position + 1
        holder.bind(item, rank, maxTickets)
    }

    override fun getItemCount(): Int = moviesList.size

    class TopMoviesViewHolder(val binding: ItemTopMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TopMoviesResponse, rank: Int, maxTickets: Int) {
            val context = binding.root.context
            binding.apply {
                txtRank.text = rank.toString()
                txtMovieTitle.text = item.title
                txtTicketCount.text = context.getString(R.string.manager_top_movies_tickets, item.totalTickets)

                val goldColor = ContextCompat.getColor(context, R.color.color_gold)
                val purpleColor = ContextCompat.getColor(context, R.color.color_bg_text_bottom)
                val textColor = ContextCompat.getColor(context, R.color.text_light_gray)

                if (rank == 1) {
                    txtRank.background?.mutate()?.setTint(goldColor)
                    txtRank.setTextColor(textColor)
                } else {
                    txtRank.background?.mutate()?.setTint(purpleColor)
                    txtRank.setTextColor(textColor)
                }

                progressMovie.max = 100
                val progressPercentage = ((item.totalTickets.toDouble() / maxTickets) * 100).toInt()
                progressMovie.progress = progressPercentage
            }
        }
    }
}


class TopMoviesDiffCallback(private val oldList: List<TopMoviesResponse>, private val newList: List<TopMoviesResponse>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].title == newList[newItemPosition].title
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItemPosition == newItemPosition && oldList[oldItemPosition] == newList[newItemPosition]
    }
}