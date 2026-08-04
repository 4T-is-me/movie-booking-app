package com.example.movie_booking_app.view.activity.manager

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.UpdateRoomRequest
import com.example.movie_booking_app.data.model.response.ManagerSeat
import com.example.movie_booking_app.data.model.response.ManagerSeatRow
import com.example.movie_booking_app.databinding.ActivityManagerSeatBinding
import com.example.movie_booking_app.databinding.ItemManagerSeatRowBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.adapter.manager.ManagerSeatAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerRoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerSeatActivity : AppCompatActivity() {

    private val viewModel: ManagerRoomViewModel by viewModels()
    private lateinit var binding: ActivityManagerSeatBinding
    private val formatMoney = DecimalFormat("#,### ₫")

    private var currentStatus: String = Constants.SeatStatus.ACTIVE
    private var pendingRegularPrice: Double = 0.0
    private var pendingVipPrice: Double = 0.0
    private var pendingCouplePrice: Double = 0.0
    private var pendingEstFullCapacity: Double = 0.0
    private var roomIdToUpdate: String = ""
    private var isEditingPricing: Boolean = false
    private var regSeatsCount: Int = 0
    private var vipSeatsCount: Int = 0
    private var cplSeatsCount: Int = 0

    private var isMultiSelectMode: Boolean = false
    private val selectedSeatIds = mutableSetOf<String>()
    private var currentSeatRows: List<ManagerSeatRow> = emptyList()

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
            currentStatus = Constants.SeatStatus.ACTIVE
            updateStatusUI(currentStatus)
        }
        binding.btnStatusMaintenance.setOnClickListener {
            currentStatus = Constants.SeatStatus.MAINTENANCE
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

        // Multi-select actions
        binding.btnToggleMultiSelect.setOnClickListener {
            toggleMultiSelectMode(!isMultiSelectMode)
        }

        binding.btnDeselectAll.setOnClickListener {
            selectedSeatIds.clear()
            updateBulkActionsUI()
            renderSeats(currentSeatRows)
        }

        binding.btnBulkActive.setOnClickListener {
            applyBulkStatus( Constants.SeatStatus.ACTIVE)
        }

        binding.btnBulkMaintenance.setOnClickListener {
            applyBulkStatus(Constants.SeatStatus.MAINTENANCE)
        }

        if (roomId != null) {
            viewModel.loadRoomSeats(roomId)
        }

        observeViewModel()
    }

    private fun toggleMultiSelectMode(enable: Boolean) {
        isMultiSelectMode = enable
        if (!enable) {
            selectedSeatIds.clear()
        }
        binding.btnToggleMultiSelect.background = ContextCompat.getDrawable(
            this,
            if (enable) R.drawable.bg_btn_status_selected else R.drawable.bg_btn_status_unselected
        )
        binding.tvMultiSelectLabel.text = if (enable) getString(R.string.manager_select_multiple_checked) else getString(R.string.auto_str_ch_n_nhi_u)
        binding.tvMultiSelectLabel.setTextColor(
            ContextCompat.getColor(
                this,
                if (enable) R.color.switch_enabled else R.color.switch_disabled
            )
        )
        updateBulkActionsUI()
        renderSeats(currentSeatRows)
    }

    private fun updateBulkActionsUI() {
        if (selectedSeatIds.isNotEmpty()) {
            binding.cvBulkActions.visibility = View.VISIBLE
            binding.tvSelectedCount.text = getString(R.string.manager_selected_seats_count, selectedSeatIds.size)
        } else {
            binding.cvBulkActions.visibility = View.GONE
        }
    }

    private fun applyBulkStatus(targetStatus: String) {
        if (selectedSeatIds.isEmpty() || roomIdToUpdate.isEmpty()) return

        val seatList = selectedSeatIds.toList()
        val label = if (targetStatus == Constants.SeatStatus.MAINTENANCE) "Bảo trì" else "Hoạt động"

        viewModel.bulkUpdateSeatStatus(roomIdToUpdate, seatList, targetStatus) { success ->
            if (success) {
                Toast.makeText(this, "Đã đổi ${seatList.size} ghế sang $label", Toast.LENGTH_SHORT).show()
                selectedSeatIds.clear()
                updateBulkActionsUI()
            } else {
                Toast.makeText(this, getString(R.string.auto_str_c_p_nh_t_tr_ng_th_i_nh_m_gh_th), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatusUI(status: String) {
        // Reset all buttons
        binding.btnStatusOperating.background = ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        binding.btnStatusOperating.backgroundTintList = null
        binding.btnStatusOperating.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.text_gray))
        binding.btnStatusOperating.text = getString(R.string.auto_str_operating)
        
        binding.btnStatusMaintenance.background = ContextCompat.getDrawable(this, R.drawable.bg_btn_status_unselected)
        binding.btnStatusMaintenance.backgroundTintList = null
        binding.btnStatusMaintenance.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.text_gray))
        binding.btnStatusMaintenance.text = getString(R.string.auto_str_maintenance)

        when (status.lowercase()) {
            "active", "operating", "open" -> {
                binding.tvTopStatus.text = getString(R.string.auto_str_operating_1)
                binding.tvTopStatus.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.color_green))
                binding.tvTopStatus.background = ContextCompat.getDrawable(this, R.drawable.bg_label_operating)
                
                binding.btnStatusOperating.background = ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                binding.btnStatusOperating.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.color_green))
                binding.btnStatusOperating.text = getString(R.string.auto_str_operating_2)
            }
            "maintenance", "maintain" -> {
                binding.tvTopStatus.text = getString(R.string.auto_str_maintenance_1)
                binding.tvTopStatus.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.auto_color_F59E0B))
                binding.tvTopStatus.background = ContextCompat.getDrawable(this, R.drawable.bg_label_maintain)
                
                binding.btnStatusMaintenance.background = ContextCompat.getDrawable(this, R.drawable.bg_btn_status_selected)
                binding.btnStatusMaintenance.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ManagerSeatActivity, R.color.auto_color_33F59E0B))
                binding.btnStatusMaintenance.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.auto_color_F59E0B))
                binding.btnStatusMaintenance.text = getString(R.string.auto_str_maintenance_2)
            }
            else -> {
                binding.tvTopStatus.text = getString(R.string.auto_str_closed_1)
                binding.tvTopStatus.setTextColor(ContextCompat.getColor(this@ManagerSeatActivity, R.color.color_error_red))
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
            binding.tvEditAction.text = getString(R.string.auto_str_done)
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
            binding.tvEditAction.text = getString(R.string.auto_str_edit)
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
                Toast.makeText(this, getString(R.string.auto_str_changes_saved_successfully), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.auto_str_failed_to_save_changes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderSeats(rows: List<ManagerSeatRow>) {
        currentSeatRows = rows
        binding.llSeats.removeAllViews()
        for (row in rows) {
            val rowBinding = ItemManagerSeatRowBinding.inflate(layoutInflater, binding.llSeats, false)
            
            rowBinding.tvRowName.text = row.row
            rowBinding.rvSeats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rowBinding.rvSeats.adapter = ManagerSeatAdapter(
                seats = row.seats ?: emptyList(),
                selectedSeatIds = selectedSeatIds,
                onSeatClick = { seat ->
                    if (isMultiSelectMode) {
                        if (selectedSeatIds.contains(seat.id)) {
                            selectedSeatIds.remove(seat.id)
                        } else {
                            selectedSeatIds.add(seat.id)
                        }
                        updateBulkActionsUI()
                        renderSeats(currentSeatRows)
                    } else {
                        showSeatStatusDialog(seat)
                    }
                },
                onSeatLongClick = { seat ->
                    if (!isMultiSelectMode) {
                        toggleMultiSelectMode(true)
                    }
                    if (selectedSeatIds.contains(seat.id)) {
                        selectedSeatIds.remove(seat.id)
                    } else {
                        selectedSeatIds.add(seat.id)
                    }
                    updateBulkActionsUI()
                    renderSeats(currentSeatRows)
                }
            )
            
            binding.llSeats.addView(rowBinding.root)
        }
    }

    private fun showSeatStatusDialog(seat: ManagerSeat) {
        val seatDisplayName = seat.seatName ?: "${seat.row ?: ""}${seat.col}"
        val options = arrayOf(getString(R.string.manager_seat_status_active_opt), getString(R.string.manager_seat_status_maintenance_opt))
        val currentSelection = if (seat.status.equals(Constants.SeatStatus.MAINTENANCE, ignoreCase = true)) 1 else 0

        var selectedOption = currentSelection

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.manager_change_seat_status_title, seatDisplayName))
            .setSingleChoiceItems(options, currentSelection) { _, which ->
                selectedOption = which
            }
            .setPositiveButton(getString(R.string.manager_confirm)) { dialog, _ ->
                val newStatus = if (selectedOption == 1) Constants.SeatStatus.MAINTENANCE else Constants.SeatStatus.ACTIVE
                if (newStatus != seat.status) {
                    if (roomIdToUpdate.isNotEmpty()) {
                        viewModel.updateSeatStatus(roomIdToUpdate, seat.id, newStatus) { success ->
                            if (success) {
                                val label = if (newStatus == Constants.SeatStatus.MAINTENANCE) getString(R.string.manager_seat_status_maintenance) else getString(R.string.manager_seat_status_active)
                                Toast.makeText(this, getString(R.string.manager_seat_status_changed_success, seatDisplayName, label), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, getString(R.string.auto_str_c_p_nh_t_tr_ng_th_i_gh_th_t_b), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.manager_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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
