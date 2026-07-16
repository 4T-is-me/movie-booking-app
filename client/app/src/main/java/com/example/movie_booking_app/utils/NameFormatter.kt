package com.example.movie_booking_app.utils

object NameFormatter {
    fun getAvatarInitials(staffName: String) : String {
        val name = staffName.trim()
        if (name.isEmpty()) return "NV"

        val parts = name.split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
            else -> name.take(2).uppercase()
        }
    }
}


