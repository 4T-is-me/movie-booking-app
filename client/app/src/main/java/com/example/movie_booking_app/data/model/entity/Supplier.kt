package com.example.movie_booking_app.data.model.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Supplier(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val isActive: Boolean = true
) : Serializable
