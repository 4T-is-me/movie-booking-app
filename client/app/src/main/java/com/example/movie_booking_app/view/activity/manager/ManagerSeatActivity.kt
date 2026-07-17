package com.example.movie_booking_app.view.activity.manager

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.UpdateRoomRequest
import com.example.movie_booking_app.view.adapter.manager.ManagerSeatAdapter
import com.example.movie_booking_app.data.model.response.ManagerSeatRow
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class ManagerSeatActivity : AppCompatActivity() {

    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var llSeats: LinearLayout
    private lateinit var progressBar: ProgressBar
    private val formatMoney = DecimalFormat("#,### ₫")

    private var currentStatus: String = "active"
    private var pendingRegularPrice: Double = 0.0
    private var pendingVipPrice: Double = 0.0
    private var pendingCouplePrice: Double = 0.0
    private var pendingEstFullCapacity: Double = 0.0
    private var roomIdToUpdate: String = ""
    private var isEditingPricing: Boolean = false
    private var regSeatsCount: Int = 0
    private var vipSeatsCount: Int = 0
    private var cplSeatsCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_seat)

        llSeats = findViewById(R.id.llSeats)
        progressBar = findViewById(R.id.progressBar)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        val roomId = intent.getStringExtra("roomId")
        if (roomId != null) roomIdToUpdate = roomId

        val roomName = intent.getStringExtra("roomName") ?: "Room Name"
        val roomType = intent.getStringExtra("roomType") ?: "Standard"
        val status = intent.getStringExtra("status") ?: "active"
        val totalSeats = intent.getIntExtra("totalSeats", 0)
        regSeatsCount = intent.getIntExtra("regularSeats", 0)
        vipSeatsCount = intent.getIntExtra("vipSeats", 0)
        cplSeatsCount = intent.getIntExtra("coupleSeats", 0)
        
        pendingRegularPrice = intent.getDoubleExtra("regularPrice", 0.0)
        pendingVipPrice = intent.getDoubleExtra("vipPrice", 0.0)
        pendingCouplePrice = intent.getDoubleExtra("couplePrice", 0.0)
        pendingEstFullCapacity = intent.getDoubleExtra("estFullCapacity", 0.0)
        
        currentStatus = status

        // Bind data
        findViewById<TextView>(R.id.tvRoomName).text = roomName
        findViewById<TextView>(R.id.tvRoomSubtitle).text = "$totalSeats seats · $roomType"
        findViewById<TextView>(R.id.tvConfigStdCount).text = regSeatsCount.toString()
        findViewById<TextView>(R.id.tvConfigVipCount).text = vipSeatsCount.toString()
        findViewById<TextView>(R.id.tvConfigCoupleCount).text = cplSeatsCount.toString()

        updateStatusUI(currentStatus)
        updatePricingUI()

        // Bind clicks for Status
        findViewById<TextView>(R.id.btnStatusOperating).setOnClickListener {
            currentStatus = "active"
            updateStatusUI(currentStatus)
        }
        findViewById<TextView>(R.id.btnStatusMaintenance).setOnClickListener {
            currentStatus = "maintenance"
            updateStatusUI(currentStatus)
        }
        findViewById<TextView>(R.id.btnStatusClosed).setOnClickListener {
            currentStatus = "inactive"
            updateStatusUI(currentStatus)
        }

        // Bind click for Edit Pricing
        findViewById<LinearLayout>(R.id.btnEditPricing).setOnClickListener {
            togglePricingEditMode()
        }

        // Bind click for Save All
        findViewById<Button>(R.id.btnSaveAll).setOnClickListener {
            saveAllChanges()
        }

        if (roomId != null) {
            viewModel.loadRoomSeats(roomId)
        }

        observeViewModel()
    }

    private fun updateStatusUI(status: String) {
        val tvTopStatus = findViewById<TextView>(R.id.tvTopStatus)
        val btnStatusOperating = findViewById<TextView>(R.id.btnStatusOperating)
        val btnStatusMaintenance = findViewById<TextView>(R.id.btnStatusMaintenance)
        val btnStatusClosed = findViewById<TextView>(R.id.btnStatusClosed)

        // Reset all buttons
        btnStatusOperating.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        btnStatusOperating.backgroundTintList = null
        btnStatusOperating.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        btnStatusOperating.text = "Operating"
        
        btnStatusMaintenance.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        btnStatusMaintenance.backgroundTintList = null
        btnStatusMaintenance.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        btnStatusMaintenance.text = "Maintenance"
        
        btnStatusClosed.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        btnStatusClosed.backgroundTintList = null
        btnStatusClosed.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        btnStatusClosed.text = "Closed"

        when (status.lowercase()) {
            "active", "operating", "open" -> {
                tvTopStatus.text = "Operating"
                tvTopStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
                tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_operating)
                
                btnStatusOperating.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                btnStatusOperating.setTextColor(android.graphics.Color.parseColor("#10B981"))
                btnStatusOperating.text = "✓ Operating"
            }
            "maintenance", "maintain" -> {
                tvTopStatus.text = "Maintenance"
                tvTopStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_maintain)
                
                btnStatusMaintenance.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                btnStatusMaintenance.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#33F59E0B"))
                btnStatusMaintenance.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                btnStatusMaintenance.text = "✓ Maintenance"
            }
            else -> {
                tvTopStatus.text = "Closed"
                tvTopStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_close)
                
                btnStatusClosed.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                btnStatusClosed.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#33EF4444"))
                btnStatusClosed.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                btnStatusClosed.text = "✓ Closed"
            }
        }
    }

    private fun updatePricingUI() {
        findViewById<TextView>(R.id.tvStdPrice).text = formatMoney.format(pendingRegularPrice)
        findViewById<TextView>(R.id.tvVipPrice).text = formatMoney.format(pendingVipPrice)
        findViewById<TextView>(R.id.tvCouplePrice).text = formatMoney.format(pendingCouplePrice)
        findViewById<TextView>(R.id.tvFullCap).text = formatMoney.format(pendingEstFullCapacity)
    }

    private fun togglePricingEditMode() {
        val tvStdPrice = findViewById<TextView>(R.id.tvStdPrice)
        val tvVipPrice = findViewById<TextView>(R.id.tvVipPrice)
        val tvCouplePrice = findViewById<TextView>(R.id.tvCouplePrice)
        
        val etStdPrice = findViewById<EditText>(R.id.etStdPrice)
        val etVipPrice = findViewById<EditText>(R.id.etVipPrice)
        val etCouplePrice = findViewById<EditText>(R.id.etCouplePrice)
        
        val tvEditAction = findViewById<TextView>(R.id.tvEditAction)
        val ivEditAction = findViewById<ImageView>(R.id.ivEditAction)
        
        val llStdEdit = findViewById<LinearLayout>(R.id.llStdEdit)
        val llVipEdit = findViewById<LinearLayout>(R.id.llVipEdit)
        val llCoupleEdit = findViewById<LinearLayout>(R.id.llCoupleEdit)
        
        isEditingPricing = !isEditingPricing
        
        if (isEditingPricing) {
            // Mode "Edit"
            tvEditAction.text = "Done"
            ivEditAction.setImageResource(android.R.drawable.ic_menu_save)
            
            // Hide Text, Show Input container
            tvStdPrice.visibility = View.GONE
            tvVipPrice.visibility = View.GONE
            tvCouplePrice.visibility = View.GONE
            
            llStdEdit.visibility = View.VISIBLE
            llVipEdit.visibility = View.VISIBLE
            llCoupleEdit.visibility = View.VISIBLE
            
            // Pre-fill input
            etStdPrice.setText(pendingRegularPrice.toLong().toString())
            etVipPrice.setText(pendingVipPrice.toLong().toString())
            etCouplePrice.setText(pendingCouplePrice.toLong().toString())
            
        } else {
            // Mode "Done"
            tvEditAction.text = "Edit"
            ivEditAction.setImageResource(android.R.drawable.ic_menu_edit)
            
            // Retrieve data
            pendingRegularPrice = etStdPrice.text.toString().toDoubleOrNull() ?: pendingRegularPrice
            pendingVipPrice = etVipPrice.text.toString().toDoubleOrNull() ?: pendingVipPrice
            pendingCouplePrice = etCouplePrice.text.toString().toDoubleOrNull() ?: pendingCouplePrice
            
            pendingEstFullCapacity = (regSeatsCount * pendingRegularPrice) + (vipSeatsCount * pendingVipPrice) + (cplSeatsCount * pendingCouplePrice)
            
            // Hide Input container, Show Text
            tvStdPrice.visibility = View.VISIBLE
            tvVipPrice.visibility = View.VISIBLE
            tvCouplePrice.visibility = View.VISIBLE
            
            llStdEdit.visibility = View.GONE
            llVipEdit.visibility = View.GONE
            llCoupleEdit.visibility = View.GONE
            
            // Update Text UI
            updatePricingUI()
        }
    }

    private fun saveAllChanges() {
        if (roomIdToUpdate.isEmpty()) return
        
        val request = UpdateRoomRequest(
            status = currentStatus,
            regularPrice = pendingRegularPrice,
            vipPrice = pendingVipPrice,
            couplePrice = pendingCouplePrice
        )
        
        viewModel.updateRoom(roomIdToUpdate, request) { success ->
            if (success) {
                Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                // Refresh room list data will be done when go back.
            } else {
                Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderSeats(rows: List<ManagerSeatRow>) {
        llSeats.removeAllViews()
        for (row in rows) {
            val rowView = layoutInflater.inflate(R.layout.item_manager_seat_row, llSeats, false)
            val tvRowName = rowView.findViewById<TextView>(R.id.tvRowName)
            val rvSeats = rowView.findViewById<RecyclerView>(R.id.rvSeats)
            
            tvRowName.text = row.row
            rvSeats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rvSeats.adapter = ManagerSeatAdapter(row.seats ?: emptyList())
            
            llSeats.addView(rowView)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.seats.collectLatest { rows ->
                renderSeats(rows)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                llSeats.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }
    }
}
