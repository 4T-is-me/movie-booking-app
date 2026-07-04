package com.example.movie_booking_app.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    fun formatMovieDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "TBA"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)
            
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            try {
                // Try parsing without milliseconds
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e2: Exception) {
                try {
                    // Try parsing simple date yyyy-MM-dd
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(dateString)
                    
                    val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    outputFormat.format(date!!)
                } catch (e3: Exception) {
                    dateString // Return original if parsing fails
                }
            }
        }
    }
}
