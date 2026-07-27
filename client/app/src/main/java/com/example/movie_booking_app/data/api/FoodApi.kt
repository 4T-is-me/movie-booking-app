package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.FoodBeverage
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface FoodApi {
    @GET("api/food-beverages")
    suspend fun getMenu(
        @Query("category") category: String? = null,
        @Query("showtimeId") showtimeId: String? = null
    ): Response<FoodMenuResponse>

    @GET("api/food-beverages/admin")
    suspend fun getAdminMenu(): Response<FoodMenuResponse>

    @Multipart
    @POST("api/food-beverages/admin")
    suspend fun addProduct(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<SingleFoodResponse>

    @Multipart
    @PUT("api/food-beverages/admin/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<SingleFoodResponse>

    @DELETE("api/food-beverages/admin/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<FoodActionResponse>
}

data class FoodMenuResponse(
    val success: Boolean,
    val data: List<FoodBeverage>
)

data class SingleFoodResponse(
    val success: Boolean,
    val data: FoodBeverage
)

data class FoodActionResponse(
    val success: Boolean,
    val message: String?
)
