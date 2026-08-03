package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.model.request.ConfirmBookingRequest
import com.example.movie_booking_app.data.model.response.PromotionResponse
import com.google.gson.JsonParser
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody

class PaymentRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    fun applyPromotion(code: String): Flow<PromotionResponse> = flow {
        val payload = mapOf("code" to code)
        val response = paymentApi.applyPromotion(payload)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Failed to validate coupon code"
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    fun confirmBooking(request: ConfirmBookingRequest): Flow<ResponseBody> = flow {
        val response = paymentApi.confirmBooking(request)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Payment confirmation failed"
            throw Exception(parseErrorMessage(errorMsg))
        }
    }

    private fun parseErrorMessage(raw: String): String {
        return try {
            if (raw.startsWith("{") && raw.contains("message")) {
                val json = JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
