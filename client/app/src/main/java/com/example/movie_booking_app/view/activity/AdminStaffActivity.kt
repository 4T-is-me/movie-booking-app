package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.data.model.request.CreateStaffRequest
import com.example.movie_booking_app.data.model.request.UpdateStaffRequest
import com.example.movie_booking_app.databinding.ActivityAdminStaffBinding
import com.example.movie_booking_app.utils.RoleConstants
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AdminStaffActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminStaffBinding
    private val viewModel: AdminStaffViewModel by viewModels()

    private var currentStaff: Staff? = null
    private var isEditMode = false

    private var cinemasList: List<Cinema> = emptyList()
    private var selectedCinemaId: String? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentStaff = intent.getSerializableExtra("staff") as? Staff
        isEditMode = currentStaff != null
        val intentRole = intent.getStringExtra("role") ?: RoleConstants.MANAGER

        setupUI(intentRole)
        setupListeners()
        observeViewModel()
    }

    private fun setupUI(intentRole: String) {
        if (isEditMode) {
            binding.tvFormTitle.text = "Update $intentRole"
            binding.layoutUsername.visibility = View.GONE
            binding.layoutPassword.visibility = View.GONE
            binding.layoutAddress.visibility = View.VISIBLE
            binding.layoutStatus.visibility = View.VISIBLE
 
            // Pre-populate fields
            currentStaff?.let { staff ->
                binding.edtFullName.setText(staff.fullName)
                binding.edtEmail.setText(staff.email)
                binding.edtPhone.setText(staff.phone)
                binding.edtIdentityCard.setText(staff.identityCard)
                binding.edtPosition.setText(staff.position)
                
                // Fix scientific notation bug (e.g. 1.5E7) by using BigDecimal toPlainString()
                val salaryText = staff.salary?.let { 
                    java.math.BigDecimal.valueOf(it).toPlainString().replace(Regex("\\.0$"), "")
                } ?: ""
                binding.edtSalary.setText(salaryText)

                binding.edtStartDate.setText(staff.startDate?.take(10) ?: "")
                binding.edtAddress.setText(staff.address)
                
                val statusText = if (staff.status != 0) "Active" else "Inactive"
                binding.actvStatus.setText(statusText, false)

                // Set identity card non-editable in edit mode
                binding.edtIdentityCard.isEnabled = false
                binding.layoutIdentityCard.alpha = 0.6f
            }
        } else {
            binding.tvFormTitle.text = "Add New $intentRole"
            binding.layoutUsername.visibility = View.VISIBLE
            binding.layoutPassword.visibility = View.VISIBLE
            binding.layoutAddress.visibility = View.VISIBLE
            binding.layoutStatus.visibility = View.GONE
        }

        // Setup Role autocomplete dropdown
        val roles = arrayOf(RoleConstants.STAFF, RoleConstants.MANAGER)
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)

        // Setup Status autocomplete dropdown
        val statuses = arrayOf("Active", "Inactive")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatus.setAdapter(statusAdapter)

        // Set default role selection if adding
        if (!isEditMode) {
            binding.actvRole.setText(intentRole, false)
        } else {
            // For editing, try to guess role from UI state (or just map to Employee/Manager based on username or endpoint)
            binding.actvRole.setText(intentRole, false)
        }
        
        // Initialize position visibility
        if (intentRole.equals(RoleConstants.MANAGER, ignoreCase = true)) {
            binding.layoutPosition.visibility = View.GONE
        } else {
            binding.layoutPosition.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        // Datepicker listener
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.edtStartDate.setText(sdf.format(calendar.time))
        }

        binding.edtStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Role dropdown listener to toggle position field and filter cinemas
        binding.actvRole.setOnItemClickListener { _, _, position, _ ->
            val selectedRole = binding.actvRole.adapter.getItem(position) as? String
            if (selectedRole.equals(RoleConstants.MANAGER, ignoreCase = true)) {
                binding.layoutPosition.visibility = View.GONE
            } else {
                binding.layoutPosition.visibility = View.VISIBLE
            }
            updateCinemaDropdown()
        }

        // Dropdown cinema listener
        binding.actvCinema.setOnItemClickListener { _, _, position, _ ->
            if (position in cinemasList.indices) {
                selectedCinemaId = cinemasList[position].id
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            validateAndSave()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.cinemas, viewModel.managers) { cinemas, managers ->
                Pair(cinemas, managers)
            }.collectLatest { (cinemas, managers) ->
                this@AdminStaffActivity.cinemasList = cinemas // original full list
                updateCinemaDropdown()
            }
        }

        lifecycleScope.launch {
            viewModel.actionSuccess.collectLatest { success ->
                if (success) {
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionMessage.collectLatest { message ->
                Toast.makeText(this@AdminStaffActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateCinemaDropdown() {
        val allCinemas = viewModel.cinemas.value
        val allManagers = viewModel.managers.value
        val isManagerMode = binding.actvRole.text.toString().equals(RoleConstants.MANAGER, ignoreCase = true)
        val managerCinemaIds = allManagers.map { it.cinemaId }.toSet()

        val filteredCinemas = if (isManagerMode && !isEditMode) {
            allCinemas.filter { it.id !in managerCinemaIds }
        } else if (isManagerMode && isEditMode) {
            allCinemas.filter { it.id !in managerCinemaIds || it.id == currentStaff?.cinemaId }
        } else {
            allCinemas
        }

        this.cinemasList = filteredCinemas
        val cinemaNames = filteredCinemas.map { it.cinemaName ?: "Unknown" }
        val cinemaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cinemaNames)
        binding.actvCinema.setAdapter(cinemaAdapter)

        // Clear selection if it's no longer valid
        if (selectedCinemaId != null && filteredCinemas.none { it.id == selectedCinemaId }) {
            binding.actvCinema.setText("", false)
            selectedCinemaId = null
        }

        // Select current staff's cinema if editing and it's valid
        if (isEditMode && currentStaff != null) {
            val matchingIndex = filteredCinemas.indexOfFirst { it.id == currentStaff?.cinemaId }
            if (matchingIndex != -1) {
                binding.actvCinema.setText(filteredCinemas[matchingIndex].cinemaName, false)
                selectedCinemaId = filteredCinemas[matchingIndex].id
            }
        }
    }

    private fun validateAndSave() {
        val fullName = binding.edtFullName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val phone = binding.edtPhone.text.toString().trim()
        val identityCard = binding.edtIdentityCard.text.toString().trim()
        val role = binding.actvRole.text.toString().trim()
        val position = binding.edtPosition.text.toString().trim()
        val salaryStr = binding.edtSalary.text.toString().trim()
        val startDate = binding.edtStartDate.text.toString().trim()

        val isManager = role.equals(RoleConstants.MANAGER, ignoreCase = true)
        val finalPosition = if (isManager) RoleConstants.MANAGER else position

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || identityCard.isEmpty() ||
            role.isEmpty() || (!isManager && position.isEmpty()) || salaryStr.isEmpty() || startDate.isEmpty() ||
            selectedCinemaId == null) {
            Toast.makeText(this, getString(R.string.auto_str_please_fill_in_all_required_fi), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.auto_str_invalid_email_address), Toast.LENGTH_SHORT).show()
            return
        }

        if (!phone.matches(Regex("^0\\d{9}$"))) {
            Toast.makeText(this, getString(R.string.auto_str_phone_number_must_start_with_0), Toast.LENGTH_SHORT).show()
            return
        }

        if (identityCard.length != 12 || !identityCard.all { it.isDigit() }) {
            Toast.makeText(this, getString(R.string.auto_str_identity_card_must_be_12_digit), Toast.LENGTH_SHORT).show()
            return
        }

        val salary = salaryStr.toDoubleOrNull()
        if (salary == null || salary < 0) {
            Toast.makeText(this, getString(R.string.auto_str_invalid_salary_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            val staffId = currentStaff?.id
            if (staffId == null) {
                Toast.makeText(this, getString(R.string.auto_str_employee_id_not_found), Toast.LENGTH_SHORT).show()
                return
            }
            val address = binding.edtAddress.text.toString().trim()
            val statusText = binding.actvStatus.text.toString()
            val statusVal = if (statusText.equals("Active", ignoreCase = true)) 1 else 0
            val request = UpdateStaffRequest(
                fullName = fullName,
                email = email,
                phone = phone,
                address = address.ifEmpty { null },
                cinemaId = selectedCinemaId!!,
                position = finalPosition,
                salary = salary,
                startDate = startDate,
                status = statusVal
            )
            viewModel.updateStaff(staffId, request)
        } else {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.auto_str_please_enter_username_and_pass), Toast.LENGTH_SHORT).show()
                return
            }

            if (username.length < 3 || username.length > 30) {
                Toast.makeText(this, getString(R.string.auto_str_username_must_be_between_3_and), Toast.LENGTH_SHORT).show()
                return
            }

            if (password.length < 6) {
                Toast.makeText(this, getString(R.string.auto_str_password_must_be_at_least_6_ch), Toast.LENGTH_SHORT).show()
                return
            }

            val address = binding.edtAddress.text.toString().trim()
            val request = CreateStaffRequest(
                username = username,
                email = email,
                password = password,
                role = role,
                fullName = fullName,
                cinemaId = selectedCinemaId!!,
                position = finalPosition,
                phone = phone,
                identityCard = identityCard,
                salary = salary,
                startDate = startDate,
                address = address.ifEmpty { null }
            )
            viewModel.createStaff(request)
        }
    }
}
