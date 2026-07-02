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
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.viewmodel.MovieViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminCreateMovieFragment : Fragment() {

    private val availableGenres = listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Science Fiction", "TV Movie", "Thriller", "War", "Western")
    private val selectedGenres = mutableSetOf<String>()

    private var selectedPosterUri: android.net.Uri? = null
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPosterUri = uri
            view?.findViewById<android.widget.ImageView>(R.id.ivPosterPreview)?.let { iv ->
                Glide.with(this).load(uri).into(iv)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_create_movie, container, false)
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        val edtMovieName = view.findViewById<TextInputEditText>(R.id.edtMovieName)
        val edtDuration = view.findViewById<TextInputEditText>(R.id.edtDuration)
        val edtReleaseDate = view.findViewById<TextInputEditText>(R.id.edtReleaseDate)
        val edtLanguage = view.findViewById<TextInputEditText>(R.id.edtLanguage)
        val edtDirector = view.findViewById<TextInputEditText>(R.id.edtDirector)
        val edtActors = view.findViewById<TextInputEditText>(R.id.edtActors)
        val edtDescription = view.findViewById<TextInputEditText>(R.id.edtDescription)

        // Setup Release Date Picker
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", Locale.US)
            edtReleaseDate.setText(sdf.format(calendar.time))
        }
        edtReleaseDate.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Setup Genres Chips
        val chipGroupGenres = view.findViewById<ChipGroup>(R.id.chipGroupGenres)
        for (genre in availableGenres) {
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                isCheckedIconVisible = false
                setTextColor(resources.getColor(android.R.color.white, null))
                setChipBackgroundColorResource(R.color.black)
                setCheckedIconTintResource(R.color.black)
                chipStrokeWidth = 2f
                setChipStrokeColorResource(R.color.text_input_stroke)
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

        // Setup Censorship Dropdown
        val censorshipOptions = arrayOf("P (Mọi lứa tuổi)", "K (Dưới 13 tuổi cần người lớn)", "T13 (Từ 13 tuổi)", "T16 (Từ 16 tuổi)", "T18 (Từ 18 tuổi)", "C (Cấm chiếu)")
        val censorshipCodes = arrayOf("P", "K", "T13", "T16", "T18", "C")
        val spnCensorship = view.findViewById<AutoCompleteTextView>(R.id.spnCensorship)
        val censorshipAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            censorshipOptions
        )
        spnCensorship.setAdapter(censorshipAdapter)

        // Setup Status Dropdown
        val statusOptions = arrayOf("showing", "coming-soon")
        val spnStatus = view.findViewById<AutoCompleteTextView>(R.id.spnStatus)
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusOptions
        )
        spnStatus.setAdapter(statusAdapter)

        // Setup Country Search Dropdown
        val countryOptions = arrayOf("Vietnam", "USA", "Korea", "Japan", "China", "UK", "France", "India", "Thailand")
        val edtCountry = view.findViewById<AutoCompleteTextView>(R.id.edtCountry)
        val countryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countryOptions
        )
        edtCountry.setAdapter(countryAdapter)

        val btnChoosePoster = view.findViewById<Button>(R.id.btnChoosePoster)
        btnChoosePoster.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitMovie)
        btnSubmit.setOnClickListener {
            val name = edtMovieName.text.toString()
            val duration = edtDuration.text.toString().toIntOrNull() ?: 0
            val releaseDate = edtReleaseDate.text.toString()
            val language = edtLanguage.text.toString()
            val director = edtDirector.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val actors = edtActors.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val description = edtDescription.text.toString()
            val country = edtCountry.text.toString()
            val status = spnStatus.text.toString()

            val censorshipRaw = spnCensorship.text.toString()
            var censorshipCode = "P"
            val index = censorshipOptions.indexOf(censorshipRaw)
            if (index >= 0) {
                censorshipCode = censorshipCodes[index]
            }

            if (name.isEmpty() || releaseDate.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = AddMovieRequest(
                movieName = name,
                duration = duration,
                releaseDate = releaseDate,
                movieGenre = selectedGenres.toList(),
                director = director,
                actors = actors,
                description = description,
                country = country,
                poster = "",
                language = language,
                censorship = censorshipCode,
                status = status
            )

            val viewModel = ViewModelProvider(requireActivity())[MovieViewModel::class.java]
            viewModel.addMovie(request)

            Toast.makeText(requireContext(), "Adding movie...", Toast.LENGTH_SHORT).show()
        }
    }
}