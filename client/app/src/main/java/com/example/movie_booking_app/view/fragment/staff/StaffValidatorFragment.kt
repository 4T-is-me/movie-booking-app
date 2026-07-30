package com.example.movie_booking_app.view.fragment.staff

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.CheckInTicketResponse
import com.example.movie_booking_app.databinding.DialogCheckInResultBinding
import com.example.movie_booking_app.databinding.DialogManualCheckInBinding
import com.example.movie_booking_app.databinding.FragmentStaffValidatorBinding
import com.example.movie_booking_app.viewmodel.staff.StaffValidatorViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

@AndroidEntryPoint
class StaffValidatorFragment : Fragment() {

    private var _binding: FragmentStaffValidatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffValidatorViewModel by viewModels()

    private var isScanningEnabled = true
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), getString(R.string.auto_str_camera_permission_is_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffValidatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startLaserAnimation()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnManualInput.setOnClickListener {
            showManualInputDialog()
        }
    }

    private fun startLaserAnimation() {
        binding.scanFrame.post {
            val scanFrame = binding.scanFrame
            val laserLine = binding.laserLine

            val animation = TranslateAnimation(
                0f, 0f,
                0f, (scanFrame.height - laserLine.height).toFloat()
            ).apply {
                duration = 2000
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
                interpolator = LinearInterpolator()
            }
            laserLine.startAnimation(animation)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningEnabled) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank() && isScanningEnabled) {
                            isScanningEnabled = false
                            requireActivity().runOnUiThread {
                                handleCheckIn(rawValue)
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleCheckIn(bookingCode: String) {
        viewModel.checkInTicket(bookingCode)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.checkInResult.collectLatest { result ->
                if (result != null) {
                    showResultDialog(result)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                // Visual feedback for API processing
                if (isLoading) {
                    binding.btnManualInput.isEnabled = false
                    binding.btnManualInput.text = getString(R.string.auto_str_validating)
                } else {
                    binding.btnManualInput.isEnabled = true
                    binding.btnManualInput.text = getString(R.string.auto_str_enter_booking_code)
                }
            }
        }
    }

    private fun showManualInputDialog() {
        isScanningEnabled = false
        val context = requireContext()
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manual_check_in, null)
        val bindingDialog = DialogManualCheckInBinding.bind(dialogView)
        
        val dialog = builder.setView(dialogView)
            .setOnCancelListener {
                isScanningEnabled = true
            }
            .create()

        bindingDialog.btnConfirm.setOnClickListener {
            val code = bindingDialog.edtBookingCode.text.toString().trim()
            if (code.isNotEmpty()) {
                handleCheckIn(code)
            } else {
                isScanningEnabled = true
            }
            dialog.dismiss()
        }

        bindingDialog.btnCancel.setOnClickListener {
            isScanningEnabled = true
            dialog.dismiss()
        }

        bindingDialog.btnCloseIcon.setOnClickListener {
            isScanningEnabled = true
            dialog.dismiss()
        }

        dialog.show()

        // Enforce explicit dialog width to prevent layout collapsing
        dialog.window?.let { window ->
            val width = (320 * context.resources.displayMetrics.density).toInt()
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun showResultDialog(response: CheckInTicketResponse) {
        val context = requireContext()
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_check_in_result, null)
        val bindingDialog = DialogCheckInResultBinding.bind(dialogView)
        
        val dialog = builder.setView(dialogView)
            .setCancelable(false)
            .create()

        if (response.success && response.data != null) {
            val data = response.data
            bindingDialog.imgStatusIcon.setImageResource(R.drawable.ic_check_circle_gold)
            bindingDialog.tvDialogTitle.text = getString(R.string.auto_str_ticket_validated)
            bindingDialog.tvDialogTitle.setTextColor(ContextCompat.getColor(context, R.color.seat_selected))
            
            bindingDialog.tvMovieName.text = data.movieName ?: "Unknown Movie"
            bindingDialog.tvCinemaRoom.text = "${data.cinemaName} • ${data.roomName} (${data.screenType})"
            bindingDialog.tvShowtime.text = "Date: ${formatShowDate(data.showDate)} • Time: ${data.showTime}"
            bindingDialog.tvSeats.text = "Seats: ${data.seatNames?.joinToString(", ") ?: "None"}"
            bindingDialog.tvBookingCode.text = "Code: ${data.bookingCode}"
            
            val comboStr = data.combos?.joinToString(", ") { "${it.name} x${it.quantity}" }
            if (!comboStr.isNullOrEmpty()) {
                bindingDialog.tvCombos.text = "Combos: $comboStr"
                bindingDialog.tvCombos.visibility = View.VISIBLE
            } else {
                bindingDialog.tvCombos.visibility = View.GONE
            }
            
            bindingDialog.tvCheckedInTime.text = "Checked In at: ${formatCheckInTime(data.checkedInAt)}"
            bindingDialog.tvCheckedInTime.visibility = View.VISIBLE
        } else {
            bindingDialog.imgStatusIcon.setImageResource(R.drawable.ic_report)
            bindingDialog.tvDialogTitle.text = getString(R.string.auto_str_validation_failed)
            bindingDialog.tvDialogTitle.setTextColor(ContextCompat.getColor(context, R.color.seat_sold))
            
            val actualMessage = if (!response.success && !response.errors.isNullOrEmpty()) {
                response.errors.firstOrNull()?.message ?: response.message
            } else {
                response.message
            }
            bindingDialog.tvMovieName.text = actualMessage ?: "Invalid Ticket"
            bindingDialog.tvCinemaRoom.text = getString(R.string.auto_str_please_check_the_booking_code)
            bindingDialog.tvShowtime.visibility = View.GONE
            bindingDialog.tvSeats.visibility = View.GONE
            bindingDialog.tvBookingCode.visibility = View.GONE
            bindingDialog.tvCombos.visibility = View.GONE
            bindingDialog.tvCheckedInTime.visibility = View.GONE
        }

        val dismissAction = View.OnClickListener {
            dialog.dismiss()
            viewModel.resetResult()
            parentFragmentManager.popBackStack()
        }
        bindingDialog.btnDismiss.setOnClickListener(dismissAction)
        bindingDialog.btnCloseIcon.setOnClickListener(dismissAction)

        dialog.show()

        // Enforce explicit dialog width to prevent layout collapsing
        dialog.window?.let { window ->
            val width = (320 * context.resources.displayMetrics.density).toInt()
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun formatShowDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "N/A"
        return try {
            val cleanDate = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val date = parser.parse(cleanDate)
            if (date != null) formatter.format(date) else cleanDate
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun formatCheckInTime(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "N/A"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val formatter = SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.ENGLISH).apply {
                timeZone = TimeZone.getDefault()
            }
            val date = parser.parse(dateStr)
            if (date != null) formatter.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "StaffValidatorFragment"
    }
}
