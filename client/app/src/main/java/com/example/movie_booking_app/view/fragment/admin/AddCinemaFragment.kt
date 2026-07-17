package com.example.movie_booking_app.view.fragment.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.viewmodel.AddCinemaViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import android.net.Uri

@AndroidEntryPoint
class AddCinemaFragment : Fragment() {

    companion object {
        private const val ARG_CINEMA_ID = "cinema_id"

        fun newInstance(cinemaId: String? = null): AddCinemaFragment {
            val fragment = AddCinemaFragment()
            val args = Bundle()
            args.putString(ARG_CINEMA_ID, cinemaId)
            fragment.arguments = args
            return fragment
        }
    }

    private var cinemaId: String? = null
    private val viewModel: AddCinemaViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var btnBack: View
    private lateinit var etCinemaName: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etHotline: TextInputEditText
    private lateinit var ivCinemaPreview: ImageView
    private lateinit var btnPickImage: MaterialButton
    private lateinit var actvStatus: AutoCompleteTextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var pbLoading: ProgressBar

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivCinemaPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cinemaId = arguments?.getString(ARG_CINEMA_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_cinema, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tvTitle)
        btnBack = view.findViewById(R.id.btnBack)
        etCinemaName = view.findViewById(R.id.etCinemaName)
        etAddress = view.findViewById(R.id.etAddress)
        etCity = view.findViewById(R.id.etCity)
        etHotline = view.findViewById(R.id.etHotline)
        ivCinemaPreview = view.findViewById(R.id.ivCinemaPreview)
        btnPickImage = view.findViewById(R.id.btnPickImage)
        actvStatus = view.findViewById(R.id.actvStatus)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)
        pbLoading = view.findViewById(R.id.pbLoading)

        if (cinemaId != null) {
            tvTitle.text = "Edit Cinema"
            btnSave.text = "Update Cinema"
            viewModel.loadCinema(cinemaId!!)
        } else {
            tvTitle.text = "Add Cinema"
            btnSave.text = "Save Cinema"
        }

        setupDropdowns()
        setupFormListeners()
        observeViewModel()
    }

    private fun setupDropdowns() {
        val statuses = arrayOf("Active", "Inactive")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        actvStatus.setAdapter(statusAdapter)
        actvStatus.setText(statuses[0], false)
        viewModel.status.value = "active"

        actvStatus.setOnItemClickListener { _, _, position, _ ->
            val selected = statuses[position]
            viewModel.status.value = selected.lowercase()
        }
    }

    private fun setupFormListeners() {
        etCinemaName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.cinemaName.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.address.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etCity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.city.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etHotline.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.hotline.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            var multipartBody: MultipartBody.Part? = null
            selectedImageUri?.let { uri ->
                val file = convertUriToFile(uri)
                if (file != null) {
                    val requestBody = file.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
                    multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)
                }
            }
            viewModel.saveCinema(multipartBody)
        }

        val onCancelClick = View.OnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnCancel.setOnClickListener(onCancelClick)
        btnBack.setOnClickListener(onCancelClick)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cinemaName.collectLatest { name ->
                if (etCinemaName.text?.toString() != name) {
                    etCinemaName.setText(name)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.address.collectLatest { addr ->
                if (etAddress.text?.toString() != addr) {
                    etAddress.setText(addr)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.city.collectLatest { city ->
                if (etCity.text?.toString() != city) {
                    etCity.setText(city)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hotline.collectLatest { hotline ->
                if (etHotline.text?.toString() != hotline) {
                    etHotline.setText(hotline)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.status.collectLatest { stat ->
                val display = if (stat.lowercase() == "active") "Active" else "Inactive"
                if (actvStatus.text?.toString() != display) {
                    actvStatus.setText(display, false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.image.collectLatest { imgUrl ->
                if (!imgUrl.isNullOrEmpty() && selectedImageUri == null) {
                    com.bumptech.glide.Glide.with(this@AddCinemaFragment)
                        .load(imgUrl)
                        .placeholder(R.drawable.bg_placeholder_card)
                        .into(ivCinemaPreview)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { msg ->
                if (msg != null) {
                    tvErrorMessage.visibility = View.VISIBLE
                    tvErrorMessage.text = msg
                } else {
                    tvErrorMessage.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    pbLoading.visibility = View.VISIBLE
                    btnSave.text = ""
                    btnSave.isEnabled = false
                } else {
                    pbLoading.visibility = View.GONE
                    btnSave.text = if (cinemaId != null) "Update Cinema" else "Save Cinema"
                    btnSave.isEnabled = true
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSuccess.collectLatest { isSuccess ->
                if (isSuccess) {
                    showSuccessDialog()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Success")
            .setMessage(if (cinemaId != null) "Cinema updated successfully." else "Cinema created successfully.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    private fun convertUriToFile(uri: Uri): File? {
        val contentResolver = requireContext().contentResolver
        val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
