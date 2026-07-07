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

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        if (isEditMode) {
            binding.tvFormTitle.text = "Cập Nhật Nhân Viên"
            binding.layoutUsername.visibility = View.GONE
            binding.layoutPassword.visibility = View.GONE
            binding.layoutAddress.visibility = View.VISIBLE

            // Pre-populate fields
            currentStaff?.let { staff ->
                binding.edtFullName.setText(staff.fullName)
                binding.edtEmail.setText(staff.email)
                binding.edtPhone.setText(staff.phone)
                binding.edtIdentityCard.setText(staff.identityCard)
                binding.edtPosition.setText(staff.position)
                binding.edtSalary.setText(staff.salary?.toString() ?: "")
                binding.edtStartDate.setText(staff.startDate?.take(10) ?: "")
                binding.edtAddress.setText(staff.address)
                
                // Set identity card non-editable in edit mode
                binding.edtIdentityCard.isEnabled = false
                binding.layoutIdentityCard.alpha = 0.6f
            }
        } else {
            binding.tvFormTitle.text = "Thêm Nhân Viên Mới"
            binding.layoutUsername.visibility = View.VISIBLE
            binding.layoutPassword.visibility = View.VISIBLE
            binding.layoutAddress.visibility = View.VISIBLE
        }

        // Setup Role autocomplete dropdown
        val roles = arrayOf("Employee", "Manager")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)

        // Set default role selection if adding
        if (!isEditMode) {
            binding.actvRole.setText("Employee", false)
        } else {
            // For editing, try to guess role from UI state (or just map to Employee/Manager based on username or endpoint)
            binding.actvRole.setText("Employee", false)
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
            viewModel.cinemas.collectLatest { cinemas ->
                cinemasList = cinemas
                val cinemaNames = cinemas.map { it.cinemaName ?: "Unknown" }
                val cinemaAdapter = ArrayAdapter(this@AdminStaffActivity, android.R.layout.simple_dropdown_item_1line, cinemaNames)
                binding.actvCinema.setAdapter(cinemaAdapter)

                // Select current staff's cinema if editing
                if (isEditMode && currentStaff != null) {
                    val matchingIndex = cinemas.indexOfFirst { it.id == currentStaff?.cinemaId }
                    if (matchingIndex != -1) {
                        binding.actvCinema.setText(cinemas[matchingIndex].cinemaName, false)
                        selectedCinemaId = cinemas[matchingIndex].id
                    }
                }
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

    private fun validateAndSave() {
        val fullName = binding.edtFullName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val phone = binding.edtPhone.text.toString().trim()
        val identityCard = binding.edtIdentityCard.text.toString().trim()
        val role = binding.actvRole.text.toString().trim()
        val position = binding.edtPosition.text.toString().trim()
        val salaryStr = binding.edtSalary.text.toString().trim()
        val startDate = binding.edtStartDate.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || identityCard.isEmpty() ||
            role.isEmpty() || position.isEmpty() || salaryStr.isEmpty() || startDate.isEmpty() ||
            selectedCinemaId == null) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (!phone.matches(Regex("^0\\d{9}$"))) {
            Toast.makeText(this, "Số điện thoại phải bắt đầu bằng số 0 và gồm 10 chữ số", Toast.LENGTH_SHORT).show()
            return
        }

        if (identityCard.length != 12 || !identityCard.all { it.isDigit() }) {
            Toast.makeText(this, "Số CMND/CCCD phải gồm 12 chữ số", Toast.LENGTH_SHORT).show()
            return
        }

        val salary = salaryStr.toDoubleOrNull()
        if (salary == null || salary < 0) {
            Toast.makeText(this, "Lương không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            val staffId = currentStaff?.id
            if (staffId == null) {
                Toast.makeText(this, "Không tìm thấy ID nhân viên", Toast.LENGTH_SHORT).show()
                return
            }
            val address = binding.edtAddress.text.toString().trim()
            val request = UpdateStaffRequest(
                fullName = fullName,
                email = email,
                phone = phone,
                address = address.ifEmpty { null },
                cinemaId = selectedCinemaId!!,
                position = position,
                salary = salary,
                startDate = startDate
            )
            viewModel.updateStaff(staffId, request)
        } else {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show()
                return
            }

            if (username.length < 3 || username.length > 30) {
                Toast.makeText(this, "Tên đăng nhập phải từ 3 đến 30 ký tự", Toast.LENGTH_SHORT).show()
                return
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show()
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
                position = position,
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
