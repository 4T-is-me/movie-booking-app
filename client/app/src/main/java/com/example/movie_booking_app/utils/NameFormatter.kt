package com.example.movie_booking_app.utils

import com.example.movie_booking_app.R

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

    fun getFallbackAvatarRes(name: String): Int {
        val actorDrawables = listOf(
            R.drawable.actor_1,
            R.drawable.actor_2,
            R.drawable.actor_3,
            R.drawable.actor_4,
            R.drawable.actor_5,
            R.drawable.actor_6,
            R.drawable.actor_7
        )
        val index = Math.abs(name.hashCode()) % actorDrawables.size
        return actorDrawables[index]
    }
}
