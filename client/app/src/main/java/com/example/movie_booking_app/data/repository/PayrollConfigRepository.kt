package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PayrollConfigApi
import com.example.movie_booking_app.data.model.response.PayrollConfigResponse
import com.example.movie_booking_app.data.model.response.UpdatePayrollConfigRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

@Singleton
class PayrollConfigRepository @Inject constructor(private val api: PayrollConfigApi) {
    fun getPayrollConfig(): Flow<Result<PayrollConfigResponse>> = executePayrollConfigRequest { api.getPayrollConfig() }

    fun updateConfig(request: UpdatePayrollConfigRequest): Flow<Result<PayrollConfigResponse>> =
        executePayrollConfigRequest { api.updatePayrollConfig(request) }

    private fun executePayrollConfigRequest(call: suspend () -> Response<PayrollConfigResponse>): Flow<Result<PayrollConfigResponse>> = flow {
        try {
            val response = call()
            if (response.isSuccessful && response.body() != null) emit(Result.success(response.body()!!))
            else emit(Result.failure(Exception(response.errorBody()?.string().orEmpty())))
        } catch (error: Exception) {
            emit(Result.failure(error))
        }
    }
}
