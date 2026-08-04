package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.request.AdminPromotionRequest
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.example.movie_booking_app.databinding.ActivityAdminEditPromotionBinding
import com.example.movie_booking_app.viewmodel.admin.AdminPromotionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminEditPromotionActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminEditPromotionBinding
    private val viewModel: AdminPromotionViewModel by viewModels()

    private var currentPromotion: AdminPromotion? = null
    private var isEditMode = false

    private val calendarStart = Calendar.getInstance()
    private val calendarEnd = Calendar.getInstance()

    private var selectedImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.edtImageUrl.setText(uri.toString())
            Glide.with(this).load(uri).into(binding.ivBannerPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminEditPromotionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPromotion = intent.getSerializableExtra("promotion") as? AdminPromotion
        isEditMode = currentPromotion != null

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup Discount Type Dropdown
        val discountTypes = arrayOf(getString(R.string.promo_percentage), getString(R.string.promo_fixed_amount))
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, discountTypes)
        binding.actvDiscountType.setAdapter(typeAdapter)

        if (isEditMode) {
            binding.tvFormTitle.text = getString(R.string.auto_str_update_promotion)
            
            binding.layoutStatus.visibility = View.VISIBLE
            val statuses = arrayOf("Active", "Inactive")
            val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
            binding.actvStatus.setAdapter(statusAdapter)
            
            currentPromotion?.let { promo ->
                binding.edtPromoTitle.setText(promo.title)
                binding.edtPromoDetail.setText(promo.detail ?: "")
                binding.edtCouponCode.setText(promo.couponCode ?: "")
                
                if (promo.discountType == "percentage") {
                    binding.actvDiscountType.setText(getString(R.string.promo_percentage), false)
                } else {
                    binding.actvDiscountType.setText(getString(R.string.promo_fixed_amount), false)
                }

                binding.edtDiscountValue.setText(promo.discountValue?.toInt()?.toString() ?: "")
                binding.edtMinOrderValue.setText(promo.minOrderValue?.toInt()?.toString() ?: "0")
                binding.edtMaxUsage.setText(promo.maxUsage?.toString() ?: "-1")
                
                if (promo.isDeleted == true) {
                    binding.actvStatus.setText("Inactive", false)
                } else {
                    binding.actvStatus.setText("Active", false)
                }
                
                val startDateSimple = formatIsoToSimpleDate(promo.startDate)
                binding.edtStartDate.setText(startDateSimple)
                parseToCalendar(startDateSimple)?.let { calendarStart.time = it }

                val endDateSimple = formatIsoToSimpleDate(promo.endDate)
                binding.edtEndDate.setText(endDateSimple)
                parseToCalendar(endDateSimple)?.let { calendarEnd.time = it }
                
                val promoImg = promo.image ?: ""
                if (promoImg.isNotEmpty()) {
                    binding.edtImageUrl.setText(promoImg)
                    Glide.with(this).load(promoImg).placeholder(R.color.card_dark).into(binding.ivBannerPreview)
                }
            }
        } else {
            binding.tvFormTitle.text = getString(R.string.auto_str_add_new_promotion)
            binding.actvDiscountType.setText(getString(R.string.promo_percentage), false)
        }
    }

    private fun setupListeners() {
        val dateStartSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendarStart.set(Calendar.YEAR, year)
            calendarStart.set(Calendar.MONTH, month)
            calendarStart.set(Calendar.DAY_OF_MONTH, day)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.edtStartDate.setText(sdf.format(calendarStart.time))
        }

        val dateEndSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendarEnd.set(Calendar.YEAR, year)
            calendarEnd.set(Calendar.MONTH, month)
            calendarEnd.set(Calendar.DAY_OF_MONTH, day)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            binding.edtEndDate.setText(sdf.format(calendarEnd.time))
        }

        binding.edtStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                dateStartSetListener,
                calendarStart.get(Calendar.YEAR),
                calendarStart.get(Calendar.MONTH),
                calendarStart.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.edtEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                dateEndSetListener,
                calendarEnd.get(Calendar.YEAR),
                calendarEnd.get(Calendar.MONTH),
                calendarEnd.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnChooseImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
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
            viewModel.actionSuccess.collectLatest { success ->
                if (success) {
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionMessage.collectLatest { message ->
                Toast.makeText(this@AdminEditPromotionActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateAndSave() {
        val title = binding.edtPromoTitle.text.toString().trim()
        val detail = binding.edtPromoDetail.text.toString().trim()
        val couponCode = binding.edtCouponCode.text.toString().trim().toUpperCase()
        val discountTypeRaw = binding.actvDiscountType.text.toString().trim()
        val discountValueStr = binding.edtDiscountValue.text.toString().trim()
        val minOrderValueStr = binding.edtMinOrderValue.text.toString().trim()
        val maxUsageStr = binding.edtMaxUsage.text.toString().trim()
        val startDateStr = binding.edtStartDate.text.toString().trim()
        val endDateStr = binding.edtEndDate.text.toString().trim()
        val imageUrl = binding.edtImageUrl.text.toString().trim()

        if (title.isEmpty() || detail.isEmpty() || couponCode.isEmpty() || discountTypeRaw.isEmpty() ||
            discountValueStr.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.auto_str_please_enter_all_required_fiel), Toast.LENGTH_SHORT).show()
            return
        }

        val discountType = if (discountTypeRaw.contains("Percentage")) "percentage" else "fixed"
        val discountValue = discountValueStr.toDoubleOrNull()
        if (discountValue == null || discountValue <= 0) {
            Toast.makeText(this, getString(R.string.auto_str_discount_value_must_be_a_posit), Toast.LENGTH_SHORT).show()
            return
        }

        if (discountType == "percentage" && discountValue > 100) {
            Toast.makeText(this, getString(R.string.auto_str_percentage_discount_cannot_exc), Toast.LENGTH_SHORT).show()
            return
        }

        val minOrderValue = minOrderValueStr.toDoubleOrNull() ?: 0.0
        if (minOrderValue < 0) {
            Toast.makeText(this, getString(R.string.auto_str_minimum_order_value_cannot_be), Toast.LENGTH_SHORT).show()
            return
        }

        val maxUsage = maxUsageStr.toIntOrNull() ?: -1

        // Parse and convert dates back to ISO format
        val isoStartDate = convertSimpleToIsoDate(startDateStr)
        val isoEndDate = convertSimpleToIsoDate(endDateStr)
        
        if (isoStartDate == null || isoEndDate == null) {
            Toast.makeText(this, getString(R.string.auto_str_invalid_date_format), Toast.LENGTH_SHORT).show()
            return
        }

        val isDeletedVal = if (isEditMode) {
            val statusStr = binding.actvStatus.text.toString().trim()
            statusStr == "Inactive"
        } else {
            null
        }

        val request = AdminPromotionRequest(
            title = title,
            detail = detail,
            couponCode = couponCode,
            discountType = discountType,
            discountValue = discountValue,
            minOrderValue = minOrderValue,
            maxUsage = maxUsage,
            startDate = isoStartDate,
            endDate = isoEndDate,
            image = imageUrl.ifEmpty { "/uploads/promotions/default.jpg" },
            isDeleted = isDeletedVal
        )

        if (isEditMode) {
            currentPromotion?.id?.let { id ->
                viewModel.updatePromotion(id, request)
            }
        } else {
            viewModel.createPromotion(request)
        }
    }

    private fun parseToCalendar(dateStr: String): Date? {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.parse(dateStr)
        } catch (e: Exception) {
            return null
        }
    }

    private fun formatIsoToSimpleDate(isoStr: String?): String {
        if (isoStr.isNullOrEmpty()) return ""
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(isoStr)
                if (date != null) {
                    val outSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    return outSdf.format(date)
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return isoStr
    }

    private fun convertSimpleToIsoDate(simpleStr: String): String? {
        if (simpleStr.isEmpty()) return null
        
        // If it's already an ISO string, return it directly
        if (simpleStr.contains("T") || simpleStr.contains("+") || (simpleStr.endsWith("Z") && simpleStr.length > 10)) {
            return simpleStr
        }
        
        val formats = arrayOf("yyyy-MM-dd", "dd/MM/yyyy", "yyyy/MM/dd")
        for (format in formats) {
            try {
                val inSdf = SimpleDateFormat(format, Locale.US)
                val date = inSdf.parse(simpleStr)
                if (date != null) {
                    val outSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    outSdf.timeZone = TimeZone.getTimeZone("UTC")
                    return outSdf.format(date)
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
    }
}
