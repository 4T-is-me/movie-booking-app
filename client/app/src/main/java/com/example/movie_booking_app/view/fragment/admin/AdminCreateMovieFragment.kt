package com.example.movie_booking_app.view.fragment.admin

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.databinding.FragmentAdminCreateMovieBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.DateUtils
import com.example.movie_booking_app.viewmodel.MovieViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AdminCreateMovieFragment : Fragment() {

    private val selectedGenres = mutableSetOf<String>()
    private var editingMovie: Movie? = null
    private var selectedPosterUri: Uri? = null

    private var _binding: FragmentAdminCreateMovieBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MovieViewModel by activityViewModels()

    private val displaySdf = SimpleDateFormat(Constants.Movie.DATE_FORMAT_DISPLAY, Locale.US)
    private val serverSdf = SimpleDateFormat(Constants.Movie.DATE_FORMAT_SERVER, Locale.US)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedPosterUri = it
            Glide.with(this).load(it).into(binding.ivPosterPreview)
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
        setupDatePickers()
        setupDropdowns()

        val availableGenres = resources.getStringArray(R.array.movie_genres)

        // Pre-fill if editing
        editingMovie = arguments?.getSerializable(Constants.Movie.EXTRA_MOVIE_DATA) as? Movie
        editingMovie?.let { fillFormWithMovieData(it) }

        setupGenreChips(availableGenres)

        binding.btnChoosePoster.setOnClickListener {
            pickImageLauncher.launch(Constants.Movie.MIME_TYPE_IMAGE)
        }

        binding.btnSubmitMovie.setOnClickListener {
            collectFormDataAndSubmit()
        }
    }

    private fun setupDatePickers() {
        binding.edtReleaseDate.setOnClickListener {
            // Only prevent past dates if it's a NEW movie. 
            // For editing, we allow historical dates.
            val minDate = if (editingMovie == null) System.currentTimeMillis() - 1000 else null
            showDatePicker(binding.edtReleaseDate, minDate)
        }

        binding.edtEndDate.setOnClickListener {
            // End date must be at least today
            showDatePicker(binding.edtEndDate, System.currentTimeMillis() - 1000)
        }
    }

    private fun showDatePicker(editText: TextInputEditText, minDate: Long? = null) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)
            editText.setText(displaySdf.format(calendar.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        minDate?.let { datePicker.datePicker.minDate = it }
        datePicker.show()
    }

    private fun setupDropdowns() {
        // Censorship
        val censorshipOptions = resources.getStringArray(R.array.censorship_options)
        setupAutoCompleteTextView(binding.spnCensorship, censorshipOptions)

        // Country
        val countryOptions = resources.getStringArray(R.array.country_options)
        setupAutoCompleteTextView(binding.edtCountry, countryOptions)

        // Language
        val languageOptions = resources.getStringArray(R.array.language_options)
        setupAutoCompleteTextView(binding.spnLanguage, languageOptions)
    }

    private fun setupAutoCompleteTextView(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }

    private fun setupGenreChips(availableGenres: Array<String>) {
        binding.chipGroupGenres.removeAllViews()
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
            binding.chipGroupGenres.addView(chip)
        }
    }

    private fun fillFormWithMovieData(movie: Movie) {
        val censorshipOptions = resources.getStringArray(R.array.censorship_options)
        val censorshipCodes = Constants.Movie.CENSORSHIP_CODES

        binding.tvFormTitle.text = getString(R.string.auto_str_update_movie)
        binding.btnSubmitMovie.text = getString(R.string.auto_str_update_movie_1)

        binding.apply {
            edtMovieName.setText(movie.movieName)
            edtDuration.setText(movie.duration?.toString())
            edtReleaseDate.setText(DateUtils.formatMovieDate(movie.releaseDate))
            edtEndDate.setText(DateUtils.formatMovieDate(movie.endDate))
            spnLanguage.setText(movie.language, false)

            edtDirector.setText(movie.director?.joinToString("${Constants.Movie.SEPARATOR_COMMA} "))
            edtActors.setText(movie.actors?.joinToString("${Constants.Movie.SEPARATOR_COMMA} "))
            edtDescription.setText(movie.description)
            edtCountry.setText(movie.country, false)

            val cIndex = censorshipCodes.indexOf(movie.censorship)
            if (cIndex >= 0) {
                spnCensorship.setText(censorshipOptions[cIndex], false)
            }

            movie.movieGenre?.forEach { selectedGenres.add(it) }

            if (!movie.poster.isNullOrEmpty()) {
                Glide.with(this@AdminCreateMovieFragment).load(movie.poster).into(ivPosterPreview)
            }
        }
    }

    private fun collectFormDataAndSubmit() {
        val name = binding.edtMovieName.text.toString()
        val duration = binding.edtDuration.text.toString().toIntOrNull() ?: 0
        val releaseDateStr = binding.edtReleaseDate.text.toString()
        val endDateStr = binding.edtEndDate.text.toString()

        val releaseDate = formatToServerDate(releaseDateStr)
        val endDate = if (endDateStr.isNotEmpty()) formatToServerDate(endDateStr) else null

        val language = binding.spnLanguage.text.toString()
        val director = parseCommaSeparatedList(binding.edtDirector.text.toString())
        val actors = parseCommaSeparatedList(binding.edtActors.text.toString())
        val description = binding.edtDescription.text.toString()
        val country = binding.edtCountry.text.toString()

        val censorshipCode = getCensorshipCode(binding.spnCensorship.text.toString())

        if (name.isEmpty() || releaseDateStr.isEmpty()) {
            showToast(getString(R.string.auto_str_please_fill_required_fields))
            return
        }

        if (editingMovie == null && isPastDate(releaseDateStr)) {
            showToast(getString(R.string.auto_str_release_date_cannot_be_in_the))
            return
        }

        val posterFile = selectedPosterUri?.let { createPosterFile(it) }

        if (editingMovie != null) {
            updateMovie(name, duration, releaseDate, endDate, language, director, actors, description, country, censorshipCode, posterFile)
        } else {
            addMovie(name, duration, releaseDate, endDate, language, director, actors, description, country, censorshipCode, posterFile)
        }
        parentFragmentManager.popBackStack()
    }

    private fun formatToServerDate(dateStr: String): String {
        return try {
            val date = displaySdf.parse(dateStr)
            serverSdf.format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseCommaSeparatedList(text: String): List<String> {
        return text.split(Constants.Movie.SEPARATOR_COMMA)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun getCensorshipCode(censorshipRaw: String): String {
        val censorshipOptions = resources.getStringArray(R.array.censorship_options)
        val censorshipCodes = Constants.Movie.CENSORSHIP_CODES
        val index = censorshipOptions.indexOf(censorshipRaw)
        return if (index >= 0) censorshipCodes[index] else Constants.Movie.DEFAULT_CENSORSHIP
    }

    private fun isPastDate(dateStr: String): Boolean {
        return try {
            val selectedDate = displaySdf.parse(dateStr)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            selectedDate != null && selectedDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    private fun addMovie(name: String, duration: Int, releaseDate: String, endDate: String?, language: String, director: List<String>, actors: List<String>, description: String, country: String, censorshipCode: String, posterFile: File?) {
        val request = AddMovieRequest(
            movieName = name, duration = duration, releaseDate = releaseDate, endDate = endDate,
            movieGenre = selectedGenres.toList(), director = director, actors = actors,
            description = description, country = country, language = language, censorship = censorshipCode
        )
        viewModel.addMovie(request, posterFile)
        showToast(getString(R.string.auto_str_adding_movie))
    }

    private fun updateMovie(name: String, duration: Int, releaseDate: String, endDate: String?, language: String, director: List<String>, actors: List<String>, description: String, country: String, censorshipCode: String, posterFile: File?) {
        val request = UpdateMovieRequest(
            movieName = name, duration = duration, releaseDate = releaseDate, endDate = endDate,
            movieGenre = selectedGenres.toList(), director = director, actors = actors,
            description = description, country = country, language = language, censorship = censorshipCode,
            isDeleted = editingMovie?.isDeleted ?: true
        )
        editingMovie?.id?.let { id ->
            val currentStatus = if (editingMovie?.isDeleted == true) Constants.Movie.STATUS_DELETED else Constants.Movie.STATUS_ACTIVE
            viewModel.updateMovie(id, request, posterFile, currentStatus)
        }
        showToast(getString(R.string.auto_str_updating_movie))
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun createPosterFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile(Constants.Movie.PREFIX_POSTER_FILE, Constants.Movie.SUFFIX_JPG, requireContext().cacheDir)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
