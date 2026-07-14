package com.example.movie_booking_app.data.repository.admin

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.model.request.*
import com.example.movie_booking_app.data.model.response.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffAdminRepository @Inject constructor(
    private val api: AdminApiService
) {
    // Staff Management
    suspend fun getStaffList(page: Int? = null, limit: Int? = null, search: String? = null): Response<AdminStaffListResponse> =
        api.getStaffList(page, limit, search)

    suspend fun createStaff(request: CreateStaffRequest): Response<Unit> = api.createStaff(request)

    suspend fun updateStaff(id: String, request: UpdateStaffRequest): Response<Unit> = api.updateStaff(id, request)

    suspend fun deleteStaff(id: String): Response<Unit> = api.deleteStaff(id)

    suspend fun updateStaffStatus(id: String, status: Int): Response<Unit> =
        api.updateStaffStatus(id, UpdateStaffStatusRequest(status))

    suspend fun updateStaffRole(id: String, role: String, roleId: String? = null): Response<Unit> =
        api.updateStaffRole(id, UpdateStaffRoleRequest(role, roleId))

    // Member Management
    suspend fun getMemberList(
        page: Int? = null,
        limit: Int? = null,
        search: String? = null,
        status: Int? = null
    ): Response<AdminMemberResponse> = api.getMemberList(page, limit, search, status)

    suspend fun adjustMemberScore(id: String, adjustment: Int, reason: String): Response<Unit> =
        api.adjustMemberScore(id, AdjustScoreRequest(adjustment, reason))

    suspend fun updateMemberStatus(id: String, status: Int): Response<Unit> =
        api.updateMemberStatus(id, UpdateMemberStatusRequest(status))

    // Others
    suspend fun getCinemas(): Response<CinemaResponse> = api.getCinemas()
}
