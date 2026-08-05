package com.example.movie_booking_app.view.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.ChatMessage
import com.example.movie_booking_app.databinding.ItemChatMessageBotBinding
import com.example.movie_booking_app.databinding.ItemChatMessageUserBinding

class ChatAdapter(
    private val onCinemaSelected: ((String) -> Unit)? = null
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2

        private object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.timestamp == newItem.timestamp && oldItem.content == newItem.content
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role.equals("user", ignoreCase = true)) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemChatMessageUserBinding.inflate(inflater, parent, false)
            UserMessageViewHolder(binding)
        } else {
            val binding = ItemChatMessageBotBinding.inflate(inflater, parent, false)
            BotMessageViewHolder(binding, onCinemaSelected)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is BotMessageViewHolder) {
            holder.bind(message)
        }
    }

    class UserMessageViewHolder(private val binding: ItemChatMessageUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessageContent.text = message.content
            binding.tvMessageTime.text = message.timestamp
        }
    }

    class BotMessageViewHolder(
        private val binding: ItemChatMessageBotBinding,
        private val onCinemaSelected: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            if (message.isLoading) {
                binding.pbBotLoading.visibility = View.VISIBLE
                binding.tvMessageContent.visibility = View.GONE
                binding.tvMessageTime.visibility = View.GONE
                binding.llCinemaOptionsContainer.visibility = View.GONE
            } else {
                binding.pbBotLoading.visibility = View.GONE
                binding.tvMessageContent.visibility = View.VISIBLE
                binding.tvMessageTime.visibility = View.VISIBLE
                binding.tvMessageContent.text = message.content
                binding.tvMessageTime.text = message.timestamp

                // Hiển thị danh sách Nút chọn rạp (Cinema Buttons) kèm địa chỉ bên trong nút
                val cinemaList = message.cinemaOptions

                if (!cinemaList.isNullOrEmpty()) {
                    binding.llCinemaOptionsContainer.removeAllViews()
                    binding.llCinemaOptionsContainer.visibility = View.VISIBLE

                    cinemaList.forEach { cinema ->
                        val cardBtn = LinearLayout(itemView.context).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(32, 20, 32, 20)
                            background = ContextCompat.getDrawable(context, R.drawable.bg_cinema_button)
                            
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                            layoutParams = params

                            // Title: 🎭 Tên rạp — Thành phố
                            val tvTitle = TextView(context).apply {
                                val headerText = if (cinema.city.isNotBlank()) "🎭 ${cinema.cinemaName} — ${cinema.city}" else "🎭 ${cinema.cinemaName}"
                                text = headerText
                                setTextColor(Color.parseColor("#F5C518"))
                                textSize = 14f
                                setTypeface(null, Typeface.BOLD)
                            }
                            addView(tvTitle)

                            // Subtitle: 📍 Địa chỉ chi tiết của rạp
                            if (cinema.address.isNotBlank()) {
                                val tvAddress = TextView(context).apply {
                                    text = "📍 ${cinema.address}"
                                    setTextColor(Color.parseColor("#B0B3C6"))
                                    textSize = 12f
                                    setPadding(0, 4, 0, 0)
                                }
                                addView(tvAddress)
                            }

                            setOnClickListener {
                                onCinemaSelected?.invoke(cinema.cinemaName)
                            }
                        }
                        binding.llCinemaOptionsContainer.addView(cardBtn)
                    }
                } else {
                    // Fallback parse text cũ nếu message không có cinemaOptions
                    val cinemaRegex = Regex("""\*\*\d+\.\*\*\s*🎭\s*\*\*([^*]+)\*\*""")
                    val matches = cinemaRegex.findAll(message.content).map { it.groupValues[1].trim() }.toList()

                    if (matches.isNotEmpty()) {
                        binding.llCinemaOptionsContainer.removeAllViews()
                        binding.llCinemaOptionsContainer.visibility = View.VISIBLE

                        matches.forEach { cinemaName ->
                            val btn = TextView(itemView.context).apply {
                                text = itemView.context.getString(R.string.chat_cinema_link, cinemaName)
                                setTextColor(Color.parseColor("#F5C518"))
                                textSize = 13f
                                setTypeface(null, Typeface.BOLD)
                                setPadding(28, 18, 28, 18)
                                background = ContextCompat.getDrawable(context, R.drawable.bg_cinema_button)
                                
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(0, 6, 0, 6)
                                }
                                layoutParams = params

                                setOnClickListener {
                                    onCinemaSelected?.invoke(cinemaName)
                                }
                            }
                            binding.llCinemaOptionsContainer.addView(btn)
                        }
                    } else {
                        binding.llCinemaOptionsContainer.removeAllViews()
                        binding.llCinemaOptionsContainer.visibility = View.GONE
                    }
                }
            }
        }
    }
}
