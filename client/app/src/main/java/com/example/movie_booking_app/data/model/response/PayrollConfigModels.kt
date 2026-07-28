package com.example.movie_booking_app.data.model.response

data class PayrollConfig(
    val _id: String? = null,
    val shiftRates: Map<String, Double> = emptyMap(),
    val holidayMultiplier: Double = 1.0,
    val managerRevenueBonus: Double = 0.0,
    val updatedAt: String? = null
)

data class PayrollConfigResponse(
    val success: Boolean,
    val message: String? = null,
    val data: PayrollConfig? = null
)

data class UpdatePayrollConfigRequest(
    val shiftRates: Map<String, Double>,
    val holidayMultiplier: Double,
    val managerRevenueBonus: Double
)
