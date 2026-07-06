package com.example.movie_booking_app.data.model.request

data class EmployeeRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val role: String = "employee",
    val status: Int = 1
)
