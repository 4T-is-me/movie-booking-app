package com.example.movie_booking_app.view.fragment.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentAdminCreateMovieBinding
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.viewmodel.MovieViewModel
import com.example.movie_booking_app.utils.DateUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.ImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminCreateMovieFragment : Fragment() {

    private val availableGenres = listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Science Fiction", "TV Movie", "Thriller", "War", "Western")
    private val selectedGenres = mutableSetOf<String>()
    private var editingMovie: com.example.movie_booking_app.data.model.entity.Movie? = null

    private var selectedPosterUri: android.net.Uri? = null
    
    private var _binding: FragmentAdminCreateMovieBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPosterUri = uri
            _binding?.ivPosterPreview?.let { iv ->
                Glide.with(this).load(uri).into(iv)
            }
        }
    }

    private fun createPosterFile(uri: android.net.Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("movie_poster_", ".jpg", requireContext().cacheDir)
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminCreateMovieBinding.inflate(inflater, container, false)
        setupViews()
        return binding.root
    }

    private fun setupViews() {
        val tvFormTitle = binding.tvFormTitle
        val edtMovieName = binding.edtMovieName
        val edtDuration = binding.edtDuration
        val edtReleaseDate = binding.edtReleaseDate
        val edtEndDate = binding.edtEndDate
        val spnLanguage = binding.spnLanguage

        val edtDirector = binding.edtDirector
        val edtActors = binding.edtActors
        val edtDescription = binding.edtDescription
        val ivPosterPreview = binding.ivPosterPreview
        val btnSubmit = binding.btnSubmitMovie

        // Setup Date Picker logic
        val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val serverSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        edtReleaseDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                edtReleaseDate.setText(displaySdf.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            // Only prevent past dates if it's a NEW movie. 
            // For editing, we allow historical dates.
            if (editingMovie == null) {
                datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            }
            datePicker.show()
        }

        edtEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                edtEndDate.setText(displaySdf.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            // End date must be at least today
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        // Setup Genres Chips
        val chipGroupGenres = binding.chipGroupGenres
        
        // Setup Censorship Dropdown
        val censorshipOptions = arrayOf("P (General Audience)", "K (Children Under 13)", "T13 (13+)", "T16 (16+)", "T18 (18+)")
        val censorshipCodes = arrayOf("P", "K", "T13", "T16", "T18", "C")
        val spnCensorship = binding.spnCensorship
        val censorshipAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            censorshipOptions
        )
        spnCensorship.setAdapter(censorshipAdapter)
        spnCensorship.setOnClickListener { spnCensorship.showDropDown() }

        // Setup Country Search Dropdown
        val countryOptions = arrayOf("Vietnam", "USA", "Korea", "Japan", "China", "UK", "France", "India", "Thailand")
        val edtCountry = binding.edtCountry
        val countryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countryOptions
        )
        edtCountry.setAdapter(countryAdapter)
        edtCountry.setOnClickListener { edtCountry.showDropDown() }

        // Setup Language Dropdown
        val languageOptions = arrayOf("Vietnamese (Dubbing)", "Vietnamese (Subtitle)", "English (Subtitle)", "English (Original)", "Korean (Subtitle)", "Japanese (Subtitle)")
        val languageAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            languageOptions
        )
        spnLanguage.setAdapter(languageAdapter)
        spnLanguage.setOnClickListener { spnLanguage.showDropDown() }

        // Pre-fill if editing
        editingMovie = arguments?.getSerializable("movie") as? com.example.movie_booking_app.data.model.entity.Movie
        editingMovie?.let { movie ->
            tvFormTitle.text = "Update Movie"
            btnSubmit.text = "UPDATE MOVIE"
            
            edtMovieName.setText(movie.movieName)
            edtDuration.setText(movie.duration?.toString())
            edtReleaseDate.setText(DateUtils.formatMovieDate(movie.releaseDate))
            edtEndDate.setText(DateUtils.formatMovieDate(movie.endDate))
            spnLanguage.setText(movie.language, false)

            edtDirector.setText(movie.director?.joinToString(", "))
            edtActors.setText(movie.actors?.joinToString(", "))
            edtDescription.setText(movie.description)
            edtCountry.setText(movie.country, false)
            
            val cIndex = censorshipCodes.indexOf(movie.censorship)
            if (cIndex >= 0) {
                spnCensorship.setText(censorshipOptions[cIndex], false)
            }
            
            movie.movieGenre?.forEach { selectedGenres.add(it) }
            
            if (!movie.poster.isNullOrEmpty()) {
                Glide.with(this).load(movie.poster).into(ivPosterPreview)
            }
        }

        // Add Chips after pre-filling selectedGenres
        for (genre in availableGenres) {
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                isCheckedIconVisible = false
                isChecked = selectedGenres.contains(genre)
                setTextColor(resources.getColor(android.R.color.white, null))
                setChipBackgroundColorResource(R.color.black)
                setCheckedIconTintResource(R.color.black)
                chipStrokeWidth = 2f
                setChipStrokeColorResource(if (isChecked) R.color.gold_selected else R.color.text_input_stroke)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGenres.add(genre)
                        setChipStrokeColorResource(R.color.gold_selected)
                    } else {
                        selectedGenres.remove(genre)
                        setChipStrokeColorResource(R.color.text_input_stroke)
                    }
                }
            }
            chipGroupGenres.addView(chip)
        }

        val btnChoosePoster = binding.btnChoosePoster
        btnChoosePoster.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            val name = edtMovieName.text.toString()
            val duration = edtDuration.text.toString().toIntOrNull() ?: 0
            val releaseDateStr = edtReleaseDate.text.toString()
            val endDateStr = edtEndDate.text.toString()
            
            val releaseDate = try {
                val date = displaySdf.parse(releaseDateStr)
                serverSdf.format(date!!)
            } catch (e: Exception) { releaseDateStr }

            val endDate = try {
                if (endDateStr.isNotEmpty()) {
                    val date = displaySdf.parse(endDateStr)
                    serverSdf.format(date!!)
                } else null
            } catch (e: Exception) { endDateStr }

            val language = spnLanguage.text.toString()
            val director = edtDirector.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val actors = edtActors.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val description = edtDescription.text.toString()
            val country = edtCountry.text.toString()


            val censorshipRaw = spnCensorship.text.toString()
            var censorshipCode = "P"
            val index = censorshipOptions.indexOf(censorshipRaw)
            if (index >= 0) {
                censorshipCode = censorshipCodes[index]
            }

            if (name.isEmpty() || releaseDateStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Additional validation for past dates when creating a new movie
            if (editingMovie == null) {
                try {
                    val selectedDate = displaySdf.parse(releaseDateStr)
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    
                    if (selectedDate != null && selectedDate.before(today)) {
                        Toast.makeText(requireContext(), "Release date cannot be in the past", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    // Ignore parse errors here as they are handled by formatting logic below
                }
            }

            val viewModel = ViewModelProvider(requireActivity())[MovieViewModel::class.java]
            val posterFile = selectedPosterUri?.let { createPosterFile(it) }

            if (editingMovie != null) {
                val request = com.example.movie_booking_app.data.model.request.UpdateMovieRequest(
                    movieName = name,
                    duration = duration,
                    releaseDate = releaseDate,
                    endDate = endDate,
                    movieGenre = selectedGenres.toList(),
                    director = director,
                    actors = actors,
                    description = description,
                    country = country,
                    language = language,
                    censorship = censorshipCode,
                    isDeleted = editingMovie?.isDeleted ?: true
                )
                editingMovie?.id?.let { id ->
                    val currentStatus = if (editingMovie?.isDeleted == true) "deleted" else "active"
                    viewModel.updateMovie(id, request, posterFile, currentStatus)
                }
                Toast.makeText(requireContext(), "Updating movie...", Toast.LENGTH_SHORT).show()
            } else {
                val request = com.example.movie_booking_app.data.model.request.AddMovieRequest(
                    movieName = name,
                    duration = duration,
                    releaseDate = releaseDate,
                    endDate = endDate,
                    movieGenre = selectedGenres.toList(),
                    director = director,
                    actors = actors,
                    description = description,
                    country = country,
                    language = language,
                    censorship = censorshipCode
                )
                viewModel.addMovie(request, posterFile)
                Toast.makeText(requireContext(), "Adding movie...", Toast.LENGTH_SHORT).show()
            }
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
