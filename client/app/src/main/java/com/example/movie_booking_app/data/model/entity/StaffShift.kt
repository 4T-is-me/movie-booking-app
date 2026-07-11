package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName

class StaffShift (
    @SerializedName("_id")
    val id: String? = null,
    val staffId: String? = null,
    val cinemaId: String? = null,
    val workDate: String? = null,
    val shift: String? = null, // 'Morning', 'Afternoon', 'Evening', 'Night'
    val status: String? = "Assigned", // 'Assigned', 'Completed', 'Absent'
    val createdAt: String? = null,
    val updatedAt: String? = null
)
