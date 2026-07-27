package com.example.movie_booking_app.view.activity.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.databinding.ActivityAdminFoodMenuBinding
import com.example.movie_booking_app.databinding.DialogAddEditFoodBinding
import com.example.movie_booking_app.view.adapter.admin.AdminFoodAdapter
import com.example.movie_booking_app.view.adapter.admin.AdminDeductionAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminFoodViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class AdminFoodMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminFoodMenuBinding
    private val viewModel: AdminFoodViewModel by viewModels()
    private lateinit var menuAdapter: AdminFoodAdapter
    
    private var selectedImageUri: Uri? = null
    private var dialogBinding: DialogAddEditFoodBinding? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogBinding?.ivPosterPreview?.let { iv ->
                Glide.with(this).load(selectedImageUri).into(iv)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminFoodMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showAddEditDialog(null) }

        menuAdapter = AdminFoodAdapter(
            onEditClick = { item -> showAddEditDialog(item) },
            onDeleteClick = { item -> showDeleteConfirm(item) }
        )

        binding.rvMenu.apply {
            layoutManager = LinearLayoutManager(this@AdminFoodMenuActivity)
            adapter = menuAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    menuAdapter.submitList(state.menuItems)

                    state.error?.let {
                        Toast.makeText(this@AdminFoodMenuActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }

                    state.successMessage?.let {
                        Toast.makeText(this@AdminFoodMenuActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showAddEditDialog(item: FoodBeverage?) {
        dialogBinding = DialogAddEditFoodBinding.inflate(LayoutInflater.from(this))
        val binding = dialogBinding!!
        val isEdit = item != null
        selectedImageUri = null

        val categories = listOf("drink", "food", "combo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Setup Deduction List
        val deductionAdapter = AdminDeductionAdapter()
        binding.rvDeductions.apply {
            layoutManager = LinearLayoutManager(this@AdminFoodMenuActivity)
            this.adapter = deductionAdapter
        }
        deductionAdapter.setSourceItems(viewModel.uiState.value.menuItems, item?.inventoryDeductions)

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isCombo = categories[position] == "combo"
                binding.layoutComboRecipe.isVisible = isCombo
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (isEdit && item != null) {
            binding.etName.setText(item.name)
            binding.etPrice.setText(item.price.toString())
            binding.etDescription.setText(item.description)
            binding.spinnerCategory.setSelection(categories.indexOf(item.category))
            binding.switchActive.isChecked = item.isActive
            
            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_placeholder_card)
                .into(binding.ivPosterPreview)
        }

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Product" else "Add New Product")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etName.text.toString()
                val price = binding.etPrice.text.toString().toIntOrNull() ?: 0
                val desc = binding.etDescription.text.toString()
                val cat = binding.spinnerCategory.selectedItem.toString()
                val active = binding.switchActive.isChecked

                if (name.isBlank()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val imagePart = selectedImageUri?.let { uri ->
                    prepareImagePart(uri)
                }

                // If single, deductions should be self. If combo, take from adapter
                val deductions = if (cat == "combo") {
                    deductionAdapter.getDeductions()
                } else {
                    item?.inventoryDeductions ?: emptyList()
                }

                viewModel.saveProduct(
                    id = item?.id,
                    name = name,
                    price = price,
                    category = cat,
                    description = desc,
                    isActive = active,
                    imagePart = imagePart,
                    inventoryDeductions = deductions
                )
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { dialogBinding = null }
            .show()
    }

    private fun prepareImagePart(uri: Uri): MultipartBody.Part? {
        val file = getFileFromUri(uri) ?: return null
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("imageUrl", file.name, requestFile)
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        // Optimized: Use a fixed name to avoid cluttering cache with multiple temp files
        val file = File(cacheDir, "temp_food_upload.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun showDeleteConfirm(item: FoodBeverage) {
        AlertDialog.Builder(this)
            .setTitle("Deactivate Product")
            .setMessage("Are you sure you want to deactivate ${item.name}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteProduct(item.id)
            }
            .setNegativeButton("No", null)
            .show()
    }
}
