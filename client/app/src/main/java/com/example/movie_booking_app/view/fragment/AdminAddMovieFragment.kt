package com.example.movie_booking_app.view.fragment

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.movie_booking_app.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class AdminAddMovieFragment : Fragment() {

    private val genresList = listOf("Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Romance", "Animation")
    private val showTimesList = listOf("09:00", "12:00", "15:00", "18:00", "21:00", "23:00")
    
    private var selectedImageUri: Uri? = null
    private lateinit var ivPosterPreview: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPosterPreview.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_add_movie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePickers(view)
        setupGenres(view)
        setupShowTimes(view)

        ivPosterPreview = view.findViewById(R.id.ivPosterPreview)

        view.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            Toast.makeText(context, "Movie Submitted!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.btnChoosePoster).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupDatePickers(view: View) {
        val edtStart = view.findViewById<TextInputEditText>(R.id.edtStartDate)
        val edtEnd = view.findViewById<TextInputEditText>(R.id.edtEndDate)

        val calendar = Calendar.getInstance()

        val startPicker = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            edtStart.setText("$day/${month + 1}/$year")
        }

        val endPicker = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            edtEnd.setText("$day/${month + 1}/$year")
        }

        edtStart.setOnClickListener {
            DatePickerDialog(requireContext(), startPicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        edtEnd.setOnClickListener {
            DatePickerDialog(requireContext(), endPicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupGenres(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupGenres)
        genresList.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                setChipBackgroundColorResource(R.color.card_dark)
                setTextColor(resources.getColor(R.color.white, null))
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupShowTimes(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupShowTimes)
        showTimesList.forEach { time ->
            val chip = Chip(requireContext()).apply {
                text = time
                isCheckable = true
                setChipBackgroundColorResource(R.color.card_dark)
                setTextColor(resources.getColor(R.color.white, null))
            }
            chipGroup.addView(chip)
        }
    }
}
