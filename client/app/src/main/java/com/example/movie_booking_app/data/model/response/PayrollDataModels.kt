package com.example.movie_booking_app.data.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class GeneratePayrollRequest(
    @SerializedName("month") val month: Int,
    @SerializedName("year") val year: Int
)

data class PayrollListResponse(
    @SerializedName("success") val success: Boolean? = true,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<PayrollItem>?
)

data class PayrollDetailResponse(
    @SerializedName("success") val success: Boolean? = true,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PayrollItem?
)

data class PayrollItem(
    @SerializedName("_id") val id: String,
    @SerializedName("employeeId") val employee: EmployeeInfo?,
    @SerializedName("cinemaId") val cinema: CinemaInfo?,
    @SerializedName("month") val month: Int,
    @SerializedName("year") val year: Int,
    @SerializedName("status") val status: String, // "Draft" or "Paid"
    @SerializedName("totalSalary") val totalSalary: Double,
    
    // Chi tiết cho Staff & Manager
    @SerializedName("shiftSalary") val shiftSalary: Double? = 0.0,
    @SerializedName("shiftCount") val shiftCount: ShiftCountDetail? = null,
    @SerializedName("penalty") val penalty: Double? = 0.0,
    @SerializedName("bonus") val bonus: Double? = 0.0,
    @SerializedName("baseSalary") val baseSalary: Double? = 0.0
) : Serializable

data class EmployeeInfo(
    @SerializedName("_id") val id: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("role") val role: String?
) : Serializable

data class CinemaInfo(
    @SerializedName("_id") val id: String,
    @SerializedName("cinemaName") val cinemaName: String?
) : Serializable

data class ShiftCountDetail(
    @SerializedName("morning") val morning: Int = 0,
    @SerializedName("afternoon") val afternoon: Int = 0,
    @SerializedName("evening") val evening: Int = 0,
    @SerializedName("night") val night: Int = 0,
    @SerializedName("absent") val absent: Int = 0
) : Serializable
