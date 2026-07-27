package com.example.movie_booking_app.view.activity.manager

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.UpdateRoomRequest
import com.example.movie_booking_app.data.model.response.ManagerSeatRow
import com.example.movie_booking_app.databinding.ActivityManagerSeatBinding
import com.example.movie_booking_app.databinding.ItemManagerSeatRowBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerSeatAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class ManagerSeatActivity : AppCompatActivity() {

    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var binding: ActivityManagerSeatBinding
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
        binding = ActivityManagerSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
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
        binding.tvRoomName.text = roomName
        binding.tvRoomSubtitle.text = "$totalSeats seats · $roomType"
        binding.tvConfigStdCount.text = regSeatsCount.toString()
        binding.tvConfigVipCount.text = vipSeatsCount.toString()
        binding.tvConfigCoupleCount.text = cplSeatsCount.toString()

        updateStatusUI(currentStatus)
        updatePricingUI()

        // Bind clicks for Status
        binding.btnStatusOperating.setOnClickListener {
            currentStatus = "active"
            updateStatusUI(currentStatus)
        }
        binding.btnStatusMaintenance.setOnClickListener {
            currentStatus = "maintenance"
            updateStatusUI(currentStatus)
        }

        // Bind click for Edit Pricing
        binding.btnEditPricing.setOnClickListener {
            togglePricingEditMode()
        }

        // Bind click for Save All
        binding.btnSaveAll.setOnClickListener {
            saveAllChanges()
        }

        if (roomId != null) {
            viewModel.loadRoomSeats(roomId)
        }

        observeViewModel()
    }

    private fun updateStatusUI(status: String) {
        // Reset all buttons
        binding.btnStatusOperating.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        binding.btnStatusOperating.backgroundTintList = null
        binding.btnStatusOperating.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        binding.btnStatusOperating.text = "Operating"
        
        binding.btnStatusMaintenance.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        binding.btnStatusMaintenance.backgroundTintList = null
        binding.btnStatusMaintenance.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        binding.btnStatusMaintenance.text = "Maintenance"

        when (status.lowercase()) {
            "active", "operating", "open" -> {
                binding.tvTopStatus.text = "Operating"
                binding.tvTopStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
                binding.tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_operating)
                
                binding.btnStatusOperating.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                binding.btnStatusOperating.setTextColor(android.graphics.Color.parseColor("#10B981"))
                binding.btnStatusOperating.text = "✓ Operating"
            }
            "maintenance", "maintain" -> {
                binding.tvTopStatus.text = "Maintenance"
                binding.tvTopStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                binding.tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_maintain)
                
                binding.btnStatusMaintenance.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                binding.btnStatusMaintenance.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#33F59E0B"))
                binding.btnStatusMaintenance.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                binding.btnStatusMaintenance.text = "✓ Maintenance"
            }
            else -> {
                binding.tvTopStatus.text = "Closed"
                binding.tvTopStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                binding.tvTopStatus.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_label_close)
            }
        }
    }

    private fun updatePricingUI() {
        binding.tvStdPrice.text = formatMoney.format(pendingRegularPrice)
        binding.tvVipPrice.text = formatMoney.format(pendingVipPrice)
        binding.tvCouplePrice.text = formatMoney.format(pendingCouplePrice)
        binding.tvFullCap.text = formatMoney.format(pendingEstFullCapacity)
    }

    private fun togglePricingEditMode() {
        isEditingPricing = !isEditingPricing
        
        if (isEditingPricing) {
            // Mode "Edit"
            binding.tvEditAction.text = "Done"
            binding.ivEditAction.setImageResource(android.R.drawable.ic_menu_save)
            
            // Hide Text, Show Input container
            binding.tvStdPrice.visibility = View.GONE
            binding.tvVipPrice.visibility = View.GONE
            binding.tvCouplePrice.visibility = View.GONE
            
            binding.llStdEdit.visibility = View.VISIBLE
            binding.llVipEdit.visibility = View.VISIBLE
            binding.llCoupleEdit.visibility = View.VISIBLE
            
            // Pre-fill input
            binding.etStdPrice.setText(pendingRegularPrice.toLong().toString())
            binding.etVipPrice.setText(pendingVipPrice.toLong().toString())
            binding.etCouplePrice.setText(pendingCouplePrice.toLong().toString())
            
        } else {
            // Mode "Done"
            binding.tvEditAction.text = "Edit"
            binding.ivEditAction.setImageResource(android.R.drawable.ic_menu_edit)
            
            // Retrieve data
            pendingRegularPrice = binding.etStdPrice.text.toString().toDoubleOrNull() ?: pendingRegularPrice
            pendingVipPrice = binding.etVipPrice.text.toString().toDoubleOrNull() ?: pendingVipPrice
            pendingCouplePrice = binding.etCouplePrice.text.toString().toDoubleOrNull() ?: pendingCouplePrice
            
            pendingEstFullCapacity = (regSeatsCount * pendingRegularPrice) + (vipSeatsCount * pendingVipPrice) + (cplSeatsCount * pendingCouplePrice)
            
            // Hide Input container, Show Text
            binding.tvStdPrice.visibility = View.VISIBLE
            binding.tvVipPrice.visibility = View.VISIBLE
            binding.tvCouplePrice.visibility = View.VISIBLE
            
            binding.llStdEdit.visibility = View.GONE
            binding.llVipEdit.visibility = View.GONE
            binding.llCoupleEdit.visibility = View.GONE
            
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
            } else {
                Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderSeats(rows: List<ManagerSeatRow>) {
        binding.llSeats.removeAllViews()
        for (row in rows) {
            val rowBinding = ItemManagerSeatRowBinding.inflate(layoutInflater, binding.llSeats, false)
            
            rowBinding.tvRowName.text = row.row
            rowBinding.rvSeats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rowBinding.rvSeats.adapter = ManagerSeatAdapter(row.seats ?: emptyList())
            
            binding.llSeats.addView(rowBinding.root)
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
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.llSeats.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }
    }
}
