package com.example.movie_booking_app.view.fragment.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.databinding.ActivityAdminFoodMenuBinding
import com.example.movie_booking_app.databinding.DialogAddEditFoodBinding
import com.example.movie_booking_app.view.adapter.admin.AdminDeductionAdapter
import com.example.movie_booking_app.view.adapter.admin.AdminFoodAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminFoodViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber


@AndroidEntryPoint
class AdminFoodMenuFragment : Fragment() {

    private var _binding: ActivityAdminFoodMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminFoodViewModel by viewModels()
    private lateinit var menuAdapter: AdminFoodAdapter
    
    private var selectedTab = TAB_ACTIVE
    private var selectedImageUri: Uri? = null
    private var dialogBinding: DialogAddEditFoodBinding? = null

    companion object {
        private const val TAB_ACTIVE = "active"
        private const val TAB_INACTIVE = "inactive"
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogBinding?.ivPosterPreview?.let { iv ->
                Glide.with(this).load(selectedImageUri).into(iv)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAdminFoodMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide local toolbar since AdminActivity has its own header
        binding.toolbar.visibility = View.GONE
        
        // Adjust constraints if necessary. Since it's ConstraintLayout, we update tabContainer constraints
        val params = binding.tabContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.topToBottom = -1
        binding.tabContainer.layoutParams = params

        setupUI()
        setupTabs()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnAdd.setOnClickListener { showAddEditDialog(null) }

        menuAdapter = AdminFoodAdapter(
            onEditClick = { item -> showAddEditDialog(item) },
            onDeleteClick = { item -> showDeleteConfirm(item) }
        )

        binding.rvMenu.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }
    }

    private fun setupTabs() {
        binding.tabActiveFood.setOnClickListener { switchTab(TAB_ACTIVE) }
        binding.tabInactiveFood.setOnClickListener { switchTab(TAB_INACTIVE) }
    }

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab

        val isActiveTab = (tab == TAB_ACTIVE)
        updateTabStyle(binding.tabActiveFood, isActiveTab)
        updateTabStyle(binding.tabInactiveFood, !isActiveTab)

        val fullList = viewModel.uiState.value.menuItems
        menuAdapter.submitList(getFilteredList(fullList))
    }

    private fun updateTabStyle(tabView: android.widget.TextView, isActive: Boolean) {
        val context = requireContext()
        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_tab_active)
            tabView.setTextColor(ContextCompat.getColor(context, R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(ContextCompat.getColor(context, R.color.text_gray))
        }
    }

    private fun getFilteredList(fullList: List<FoodBeverage>): List<FoodBeverage> {
        return if (selectedTab == TAB_ACTIVE) {
            fullList.filter { it.isActive }
        } else {
            fullList.filter { !it.isActive }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    menuAdapter.submitList(getFilteredList(state.menuItems))

                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }

                    state.successMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.resetStatus()
                    }
                }
            }
        }
    }

    private fun showAddEditDialog(item: FoodBeverage?) {
        dialogBinding = DialogAddEditFoodBinding.inflate(LayoutInflater.from(requireContext()))
        val binding = dialogBinding!!
        val isEdit = item != null
        selectedImageUri = null
        
        binding.switchActive.isVisible = !isEdit

        val categories = listOf("drink", "food", "combo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        val deductionAdapter = AdminDeductionAdapter()
        binding.rvDeductions.apply {
            layoutManager = LinearLayoutManager(requireContext())
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

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Product" else "Add New Product")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etName.text.toString()
                val price = binding.etPrice.text.toString().toIntOrNull() ?: 0
                val desc = binding.etDescription.text.toString()
                val cat = binding.spinnerCategory.selectedItem.toString()
                val active = binding.switchActive.isChecked

                if (name.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.auto_str_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val imagePart = selectedImageUri?.let { uri ->
                    prepareImagePart(uri)
                }

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
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().cacheDir, "temp_food_upload.jpg")
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
                file
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating temp file from URI")
            null
        }
    }

    private fun showDeleteConfirm(item: FoodBeverage) {
        val isActive = item.isActive
        val title = if (isActive) "Deactivate Product" else "Reactivate Product"
        val message = if (isActive) 
            "Are you sure you want to deactivate ${item.name}?" 
        else 
            "Are you sure you want to reactivate ${item.name}?"

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                if (isActive) {
                    viewModel.deleteProduct(item.id)
                } else {
                    viewModel.saveProduct(
                        id = item.id,
                        name = item.name,
                        price = item.price,
                        category = item.category,
                        description = item.description ?: "",
                        isActive = true,
                        imagePart = null,
                        inventoryDeductions = item.inventoryDeductions ?: emptyList()
                    )
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
