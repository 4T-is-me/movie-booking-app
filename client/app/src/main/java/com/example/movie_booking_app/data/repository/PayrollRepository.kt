package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.PayrollApiService
import com.example.movie_booking_app.data.model.response.GeneratePayrollRequest
import com.example.movie_booking_app.data.model.response.GenericMessageResponse
import com.example.movie_booking_app.data.model.response.PayrollDetailResponse
import com.example.movie_booking_app.data.model.response.PayrollListResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class PayrollRepository @Inject constructor(
    private val api: PayrollApiService
) {
    fun generatePayroll(month: Int, year: Int): Flow<Result<GenericMessageResponse>> = flow {
        try {
            val response = api.generatePayroll(GeneratePayrollRequest(month, year))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to generate payroll"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getPayrolls(
        month: Int?,
        year: Int?,
        cinemaId: String?,
        status: String?
    ): Flow<Result<PayrollListResponse>> = flow {
        try {
            val response = api.getPayrolls(month, year, cinemaId, status)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch payroll list"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getManagerPayrolls(): Flow<Result<PayrollListResponse>> = flow {
        try {
            val response = api.getManagerPayrolls()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch manager payrolls"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getStaffPayrolls(): Flow<Result<PayrollListResponse>> = flow {
        try {
            val response = api.getStaffPayrolls()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch staff payrolls"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }


    fun getPayrollById(id: String): Flow<Result<PayrollDetailResponse>> = flow {
        try {
            val response = api.getPayrollById(id)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch payroll details"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun markAsPaid(id: String): Flow<Result<GenericMessageResponse>> = flow {
        try {
            val response = api.markAsPaid(id)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to mark payroll as paid"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
