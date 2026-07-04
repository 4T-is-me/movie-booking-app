package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityEditProfileBinding
import com.example.movie_booking_app.viewmodel.EditProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val genderOptions = arrayOf("Male", "Female", "Other")
    private val cinemaOptions = arrayOf(
        "CGV Vincom Center",
        "BHD Star 3/2",
        "Lotte Cinema Cantavil",
        "Galaxy Nguyen Du",
        "Beta Cinema"
    )
    private val genreOptions = arrayOf(
        "Action", "Comedy", "Drama", "Horror", "Romance",
        "Sci-Fi", "Thriller", "Animation", "Adventure", "Fantasy"
    )

    private val selectedGenres = mutableListOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = getFileFromUri(it)
            if (file != null) {
                viewModel.uploadAvatar(file)
            } else {
                Toast.makeText(this, "Failed to load selected image", Toast.LENGTH_SHORT).show()
            }
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
            navigateToMainSection("Home")
        }
        binding.btnNavTicket.setOnClickListener {
            navigateToMainSection("Ticket")
        }
        binding.btnNavMovie.setOnClickListener {
            navigateToMainSection("Movie")
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
                    binding.btnSaveChanges.isEnabled = !state.isSaving

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
                            "http://192.168.9.144:5000$url"
                        }
                        Glide.with(this@EditProfileActivity)
                            .load(finalUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding.ivAvatar)
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
                        Snackbar.make(binding.root, "Profile updated successfully.", Snackbar.LENGTH_SHORT).show()
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

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("avatar_temp_", ".jpg", cacheDir)
            tempFile.deleteOnExit()
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
