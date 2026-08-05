package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityEditProfileBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.viewmodel.EditProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val genderOptions = Constants.GENDER_OPTIONS
    private val cinemaOptions = Constants.CINEMA_OPTIONS
    private val genreOptions = Constants.GENRE_OPTIONS

    private val selectedGenres = mutableListOf<String>()
    private var lastLoadedAvatarUrl: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processAndUploadAvatar(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // Change photo triggers
        val choosePhotoClick = View.OnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        binding.ivAvatar.setOnClickListener(choosePhotoClick)
        binding.btnChangeAvatar.setOnClickListener(choosePhotoClick)

        // Date of Birth DatePicker trigger
        binding.fieldDateOfBirth.setFieldOnClickListener {
            showDatePicker()
        }

        // Gender list selection trigger
        binding.fieldGender.setFieldOnClickListener {
            showGenderSelector()
        }

        // Favorite Cinema Dropdown trigger
        binding.fieldFavoriteCinema.setFieldOnClickListener {
            showCinemaSelector()
        }

        // Favorite Genres list multiselect trigger
        binding.fieldFavoriteGenre.setFieldOnClickListener {
            showGenresSelector()
        }

        // Save Changes click
        binding.btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        // Fixed bottom navigation click listeners
        binding.btnNavHome.setOnClickListener {
            navigateToMainSection(Constants.NAV.HOME)
        }
        binding.btnNavTicket.setOnClickListener {
            navigateToMainSection(Constants.NAV.TICKET)
        }
        binding.btnNavMovie.setOnClickListener {
            navigateToMainSection(Constants.NAV.MOVIE)
        }
        binding.btnNavProfile.setOnClickListener {
            // Already editing profile; just close this screen to return to profile
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle overlays / loading states
                    val isRunning = state.isLoading || state.isSaving || state.isUploadingAvatar
                    binding.layoutLoading.visibility = if (isRunning) View.VISIBLE else View.GONE
                    binding.btnSaveChanges.isEnabled = !isRunning

                    // Populate user profile info when loaded
                    state.userProfile?.let { profile ->
                        if (binding.fieldFullName.getValue().isEmpty()) {
                            binding.fieldFullName.setValue(profile.fullName ?: "")
                            binding.fieldDateOfBirth.setValue(formatDateString(profile.dob))
                            binding.fieldGender.setValue(profile.gender ?: "")
                            binding.fieldPhone.setValue(profile.phone ?: "")
                            binding.fieldIdentityCard.setValue(profile.identityCard ?: "")
                            binding.fieldEmail.setValue(profile.email ?: "")
                            binding.fieldAddress.setValue(profile.address ?: "")
                            binding.fieldFavoriteCinema.setValue(profile.favoriteCinema ?: "")

                            selectedGenres.clear()
                            profile.favoriteGenres?.let { selectedGenres.addAll(it) }
                            binding.fieldFavoriteGenre.setValue(selectedGenres.joinToString(", "))
                        }
                    }

                    // Load avatar using Glide
                    state.avatarUrl?.let { url ->
                        val finalUrl = if (url.startsWith("http")) {
                            url
                        } else {
                            // Prepend base URL for local paths like /uploads/avatar.jpg
                            "${Constants.BASE_URL.removeSuffix("/")}$url"
                        }
                        if (finalUrl != lastLoadedAvatarUrl) {
                            lastLoadedAvatarUrl = finalUrl
                            Glide.with(this@EditProfileActivity)
                                .load(finalUrl)
                                .signature(ObjectKey(System.currentTimeMillis().toString()))
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(binding.ivAvatar)
                        }
                    }

                    // Display errors on inputs
                    binding.fieldFullName.getEditText().error = state.nameError
                    binding.fieldDateOfBirth.getEditText().error = state.dateOfBirthError
                    binding.fieldGender.getEditText().error = null
                    binding.fieldPhone.getEditText().error = state.phoneError
                    binding.fieldIdentityCard.getEditText().error = state.identityCardError
                    binding.fieldAddress.getEditText().error = state.addressError

                    // Handle success state
                    if (state.saveSuccess) {
                        viewModel.resetSaveSuccess()
                        Snackbar.make(binding.root, getString(R.string.profile_update_success), Snackbar.LENGTH_SHORT).show()
                        binding.root.postDelayed({
                            finish()
                        }, 1200)
                    }

                    // Handle exception error state
                    state.error?.let { err ->
                        viewModel.resetError()
                        Snackbar.make(binding.root, err, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentDob = binding.fieldDateOfBirth.getValue()
        if (currentDob.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.parse(currentDob)?.let { date ->
                    calendar.time = date
                }
            } catch (e: Exception) {
                // ignore and use default current date
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.fieldDateOfBirth.setValue(format.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showGenderSelector() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Gender")
        builder.setItems(genderOptions) { _, which ->
            binding.fieldGender.setValue(genderOptions[which])
        }
        builder.show()
    }

    private fun showCinemaSelector() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Favorite Cinema")
        builder.setItems(cinemaOptions) { _, which ->
            binding.fieldFavoriteCinema.setValue(cinemaOptions[which])
        }
        builder.show()
    }

    private fun showGenresSelector() {
        val checkedItems = BooleanArray(genreOptions.size) { index ->
            selectedGenres.contains(genreOptions[index])
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Favorite Genres")
        builder.setMultiChoiceItems(genreOptions, checkedItems) { _, which, isChecked ->
            val genre = genreOptions[which]
            if (isChecked) {
                if (!selectedGenres.contains(genre)) {
                    selectedGenres.add(genre)
                }
            } else {
                selectedGenres.remove(genre)
            }
        }
        builder.setPositiveButton("OK") { _, _ ->
            binding.fieldFavoriteGenre.setValue(selectedGenres.joinToString(", "))
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveChanges() {
        val fullName = binding.fieldFullName.getValue()
        val dob = binding.fieldDateOfBirth.getValue()
        val gender = binding.fieldGender.getValue()
        val phone = binding.fieldPhone.getValue()
        val identityCard = binding.fieldIdentityCard.getValue()
        val email = binding.fieldEmail.getValue()
        val address = binding.fieldAddress.getValue()
        val favoriteCinema = binding.fieldFavoriteCinema.getValue()

        viewModel.saveProfile(
            fullName = fullName,
            dob = dob,
            gender = gender,
            phone = phone,
            identityCard = identityCard,
            email = email,
            address = address,
            favoriteCinema = favoriteCinema,
            favoriteGenres = selectedGenres
        )
    }

    private fun navigateToMainSection(section: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("TARGET_SECTION", section)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun processAndUploadAvatar(uri: Uri) {
        viewModel.setUploadingState(true)
        lifecycleScope.launch {
            try {
                val processedFile = withContext(Dispatchers.Default) {
                    val bitmap = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    } ?: throw Exception("Failed to decode bitmap from Uri")

                    val resizedBitmap = resizeBitmap(bitmap, 800, 800)

                    val tempFile = withContext(Dispatchers.IO) {
                        File.createTempFile("avatar_compressed_", ".jpg", cacheDir).apply {
                            deleteOnExit()
                        }
                    }

                    withContext(Dispatchers.IO) {
                        FileOutputStream(tempFile).use { out ->
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                    }

                    tempFile
                }

                if (!isDestroyed && !isFinishing) {
                    viewModel.uploadAvatar(processedFile)
                }
            } catch (e: Exception) {
                Log.e("EditProfileActivity", "Error processing image", e)
                if (!isDestroyed && !isFinishing) {
                    viewModel.setUploadingState(false)
                    Toast.makeText(this@EditProfileActivity, getString(R.string.auto_str_unable_to_process_selected_ima), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxWidth && height <= maxHeight) {
            return source
        }
        val ratio = width.toFloat() / height.toFloat()
        var targetWidth = maxWidth
        var targetHeight = maxHeight
        if (ratio > 1) {
            targetHeight = (maxWidth / ratio).toInt()
        } else {
            targetWidth = (maxHeight * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun formatDateString(rawDate: String?): String {
        if (rawDate.isNullOrEmpty()) return ""
        return try {
            // If API returns ISO format (yyyy-MM-ddThh:mm:ss...) parse and convert to dd/MM/yyyy
            val inputFormat = if (rawDate.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            } else if (rawDate.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(rawDate)
            if (parsedDate != null) outputFormat.format(parsedDate) else rawDate
        } catch (e: Exception) {
            rawDate
        }
    }
}
