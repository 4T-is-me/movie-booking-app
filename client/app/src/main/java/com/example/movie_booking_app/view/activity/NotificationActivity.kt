package com.example.movie_booking_app.view.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.NotificationItem
import com.example.movie_booking_app.data.local.NotificationLocalStorage
import com.example.movie_booking_app.databinding.ActivityNotificationBinding
import com.example.movie_booking_app.databinding.ItemNotificationBinding
import dagger.hilt.android.AndroidEntryPoint

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.data.api.MaintenanceApi
import com.example.movie_booking_app.data.api.ResolveMaintenanceRequest
import com.example.movie_booking_app.data.api.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

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

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()

        // If launched via notification click from background, extract the payload and persist it
        val title = intent.getStringExtra("title")
            ?: intent.getStringExtra("gcm.notification.title")
            ?: intent.getStringExtra("google.c.a.c_l") // Some FCM versions use this for title
            ?: "Notification"

        val body = intent.getStringExtra("body")
            ?: intent.getStringExtra("message")
            ?: intent.getStringExtra("gcm.notification.body")
            ?: ""

        val type = intent.getStringExtra("type")
            ?: intent.getStringExtra("notificationType")
            ?: NotificationItem.TYPE_GENERAL

        val referenceId = intent.getStringExtra("referenceId")
            ?: intent.getStringExtra("refId")
            ?: intent.getStringExtra("bookingId")

        val shiftId = intent.getStringExtra("shiftId")
        val leaveRequestId = intent.getStringExtra("leaveRequestId")

        if (title != "Notification" || !body.isNullOrEmpty()) {
            val loaded = NotificationLocalStorage.getAll(this)
            // Check for duplicates within a short time window or by content
            val alreadyExists = loaded.take(5).any { it.title == title && it.body == body }
            if (!alreadyExists) {
                val item = NotificationItem(
                    title = title,
                    body = body,
                    type = type,
                    referenceId = referenceId,
                    shiftId = shiftId,
                    leaveRequestId = leaveRequestId
                )
                NotificationLocalStorage.save(this, item)
            }
        }

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
        ContextCompat.registerReceiver(
            this,
            notificationReceiver,
            IntentFilter("com.example.movie_booking_app.NEW_NOTIFICATION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Ignore
        }
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

            // Added by AI Assistant - Maintenance & Voucher Dialogs - 2026-07-26
            val isVoucherOrPromo = item.type.equals("PROMOTION", ignoreCase = true) ||
                    item.title.contains("Voucher", ignoreCase = true) ||
                    item.title.contains("Khuyến mãi", ignoreCase = true) ||
                    item.body.contains("Mã voucher", ignoreCase = true)

            val isMaintenanceAction = !isVoucherOrPromo && (
                    item.type.contains("MAINTENANCE_ACTION_REQUIRED", ignoreCase = true) ||
                    item.type.equals("ACTION_REQUIRED", ignoreCase = true) ||
                    (item.title.contains("Sự cố", ignoreCase = true) && item.title.contains("dời vé", ignoreCase = true))
            )

            if (isVoucherOrPromo) {
                showVoucherDialog(item)
            } else if (isMaintenanceAction) {
                showMaintenanceDialog(item)
            } else if (!item.referenceId.isNullOrEmpty()) {
                when (item.type) {
                    NotificationItem.TYPE_BOOKING -> {
                        val intent = Intent(this, BookingDetailActivity::class.java).apply {
                            putExtra("BOOKING_ID", item.referenceId)
                            putExtra("FROM_NOTIFICATION", true)
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    // Added by AI Assistant - Maintenance Action Dialog - 2026-07-26
    private fun showMaintenanceDialog(item: NotificationItem) {
        var bookingId = item.referenceId

        if (!bookingId.isNullOrEmpty()) {
            displayMaintenanceDialogUI(item, bookingId)
        } else {
            // Auto fetch bookingId from backend Notification API if missing locally
            lifecycleScope.launch {
                try {
                    val res = RetrofitClient.notificationApi.getNotifications(limit = 10)
                    if (res.isSuccessful && res.body() != null) {
                        val items = res.body()?.data ?: emptyList()
                        val match = items.firstOrNull { it.type == "ACTION_REQUIRED" || (it.metadata != null && it.metadata.containsKey("bookingId")) }
                        val foundId = match?.metadata?.get("bookingId")?.toString()
                        if (!foundId.isNullOrEmpty()) {
                            bookingId = foundId
                        }
                    }
                } catch (e: Exception) {
                    // Ignore error
                }
                displayMaintenanceDialogUI(item, bookingId)
            }
        }
    }

    private fun displayMaintenanceDialogUI(item: NotificationItem, bookingId: String?) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val dialogBinding = com.example.movie_booking_app.databinding.DialogMaintenanceActionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDialogTitle.text = item.title
        dialogBinding.tvDialogMessage.text = item.body

        if (!bookingId.isNullOrEmpty()) {
            dialogBinding.btnAccept.setOnClickListener {
                resolveMaintenanceAction(bookingId, "accept")
                dialog.dismiss()
            }

            dialogBinding.btnDecline.setOnClickListener {
                resolveMaintenanceAction(bookingId, "decline")
                dialog.dismiss()
            }
        } else {
            dialogBinding.btnAccept.visibility = android.view.View.GONE
            dialogBinding.btnDecline.visibility = android.view.View.GONE
        }

        dialogBinding.tvDismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Added by AI Assistant - Pure Voucher/Promotion Info Dialog (No Accept/Decline, Only Close) - 2026-07-26
    private fun showVoucherDialog(item: NotificationItem) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val dialogBinding = com.example.movie_booking_app.databinding.DialogMaintenanceActionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDialogTitle.text = item.title
        dialogBinding.tvDialogMessage.text = item.body

        // Hide Agree and Decline buttons completely for Voucher notifications
        dialogBinding.btnAccept.visibility = android.view.View.GONE
        dialogBinding.btnDecline.visibility = android.view.View.GONE

        // Display a clean Exit Button
        dialogBinding.tvDismiss.text = "✕  Đóng"
        dialogBinding.tvDismiss.textSize = 16f
        dialogBinding.tvDismiss.setTextColor(ContextCompat.getColor(this, R.color.gold_selected))
        dialogBinding.tvDismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun resolveMaintenanceAction(bookingId: String, action: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.maintenanceApi.resolveMaintenance(
                    bookingId,
                    ResolveMaintenanceRequest(action)
                )
                if (response.isSuccessful) {
                    val msg = if (action == "accept") {
                        "Cảm ơn bạn! Đã xác nhận dời vé thành công. Voucher đền bù đã được gửi vào ví."
                    } else {
                        "Đã từ chối dời vé! 100% số điểm thưởng đã được hoàn trả vào ví của bạn."
                    }
                    Toast.makeText(this@NotificationActivity, msg, Toast.LENGTH_LONG).show()
                    loadNotifications()
                } else {
                    val errorMsg = if (response.code() == 409) {
                        "Thông báo: Vé này đã được xác nhận hoặc hủy trước đó."
                    } else {
                        "Lỗi xử lý dời vé (Mã: ${response.code()})"
                    }
                    Toast.makeText(this@NotificationActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationActivity, "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
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
            val binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return NotifViewHolder(binding)
        }

        override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
            holder.bind(list[position], onItemClick)
        }

        override fun getItemCount(): Int = list.size

        class NotifViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: NotificationItem, onClick: (NotificationItem) -> Unit) {
                binding.tvNotifTitle.text = item.title
                binding.tvNotifBody.text = item.body

                // Set type-specific icon
                val iconRes = when (item.type) {
                    NotificationItem.TYPE_BOOKING -> R.drawable.ic_ticket
                    NotificationItem.TYPE_PROMOTION -> R.drawable.ic_gift
                    else -> R.drawable.ic_notifications
                }
                binding.ivNotifIcon.setImageResource(iconRes)

                // Unread dot indicator
                binding.viewUnreadIndicator.visibility = if (item.isRead) View.GONE else View.VISIBLE

                // Relative times formatting (e.g. "5 minutes ago")
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvNotifTime.text = relativeTime

                binding.root.setOnClickListener {
                    onClick(item)
                }
                binding.layoutContainer.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }
}
