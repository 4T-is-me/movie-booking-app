package com.example.movie_booking_app.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.NotificationItem
import com.example.movie_booking_app.data.local.NotificationLocalStorage
import com.example.movie_booking_app.databinding.ActivityNotificationBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity displaying the user's local push notification history.
 *
 * Automatically marks notifications as read when clicked and routes the user
 * to the appropriate detail screen (e.g., BookingDetailActivity) if applicable.
 */
@AndroidEntryPoint
class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var adapter: NotificationAdapter
    private var notificationList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.ivClearAll.setOnClickListener {
            NotificationLocalStorage.clearAll(this)
            loadNotifications()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList) { item ->
            // Mark as read when clicked
            NotificationLocalStorage.markRead(this, item.id)
            loadNotifications() // refresh indicators

            // Navigate to appropriate detail activity
            if (!item.referenceId.isNullOrEmpty()) {
                when (item.type) {
                    NotificationItem.TYPE_BOOKING -> {
                        val intent = Intent(this, BookingDetailActivity::class.java).apply {
                            putExtra("BOOKING_ID", item.referenceId)
                            putExtra("FROM_NOTIFICATION", true)
                        }
                        startActivity(intent)
                    }
                    NotificationItem.TYPE_PROMOTION -> {
                        // Open MainActivity redirected to the Promo section
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("TARGET_SECTION", "Promo")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val loaded = NotificationLocalStorage.getAll(this)
        notificationList.clear()
        notificationList.addAll(loaded)

        if (notificationList.isEmpty()) {
            binding.rvNotifications.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.ivClearAll.visibility = View.GONE
        } else {
            binding.rvNotifications.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.ivClearAll.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    // ────────────────────────── Notification Adapter ──────────────────────────

    class NotificationAdapter(
        private val list: List<NotificationItem>,
        private val onItemClick: (NotificationItem) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotifViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
            holder.bind(list[position], onItemClick)
        }

        override fun getItemCount(): Int = list.size

        class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val layoutContainer: View = itemView.findViewById(R.id.layoutContainer)
            private val ivNotifIcon: ImageView = itemView.findViewById(R.id.ivNotifIcon)
            private val tvNotifTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
            private val tvNotifBody: TextView = itemView.findViewById(R.id.tvNotifBody)
            private val tvNotifTime: TextView = itemView.findViewById(R.id.tvNotifTime)
            private val viewUnreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)

            fun bind(item: NotificationItem, onClick: (NotificationItem) -> Unit) {
                tvNotifTitle.text = item.title
                tvNotifBody.text = item.body

                // Set type-specific icon
                val iconRes = when (item.type) {
                    NotificationItem.TYPE_BOOKING -> R.drawable.ic_ticket
                    NotificationItem.TYPE_PROMOTION -> R.drawable.ic_gift
                    else -> R.drawable.ic_notifications
                }
                ivNotifIcon.setImageResource(iconRes)

                // Unread dot indicator
                viewUnreadIndicator.visibility = if (item.isRead) View.GONE else View.VISIBLE

                // Relative times formatting (e.g. "5 minutes ago")
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                tvNotifTime.text = relativeTime

                layoutContainer.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }
}
