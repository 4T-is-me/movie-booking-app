package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.MyApplication
import com.example.movie_booking_app.utils.Constants
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 is the special alias to your host loopback interface (localhost) for Android emulator
    // Note for LDPlayer: If 10.0.2.2 doesn't work, change this to your PC's current IPv4 address (run ipconfig to check).

    private const val BASE_URL = Constants.BASE_URL


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor by lazy {
        AuthInterceptor(MyApplication.getContext())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        instance.create(AuthApi::class.java)
    }

    val movieApi: MovieApi by lazy {
        instance.create(MovieApi::class.java)
    }

    val profileApi: ProfileApi by lazy {
        instance.create(ProfileApi::class.java)
    }

    val paymentApi: PaymentApi by lazy {
        instance.create(PaymentApi::class.java)
    }

    val cinemaRoomApi: CinemaRoomApi by lazy {
        instance.create(CinemaRoomApi::class.java)
    }

    val adminApi: AdminApiService by lazy {
        instance.create(AdminApiService::class.java)
    }

    val bookingApi: BookingApi by lazy {
        instance.create(BookingApi::class.java)
    }

    val managerRoomApi: ManagerRoomApi by lazy {
        instance.create(ManagerRoomApi::class.java)
    }

    val managerApi: ManagerApi by lazy {
        instance.create(ManagerApi:: class.java)
    }

    val fcmApi: FcmApi by lazy {
        instance.create(FcmApi::class.java)
    }

    val memberApi: MemberApi by lazy {
        instance.create(MemberApi::class.java)
    }

    val staffApi: StaffApi by lazy {
        instance.create(StaffApi::class.java)
    }
    val leaveRequestApi: LeaveRequestApi by lazy {
        instance.create(LeaveRequestApi::class.java)
    }

    val notificationApi: NotificationApi by lazy {
        instance.create(NotificationApi::class.java)
    }

    val foodApi: FoodApi by lazy {
        instance.create(FoodApi::class.java)
    }

    val inventoryApi: InventoryApi by lazy {
        instance.create(InventoryApi::class.java)
    }

    val payrollApi: PayrollApi by lazy {
        instance.create(PayrollApi::class.java)
    }

    val payrollConfigApi: PayrollConfigApi by lazy {
        instance.create(PayrollConfigApi::class.java)
    }

    val maintenanceApi: MaintenanceApi by lazy {
        instance.create(MaintenanceApi::class.java)
    }

    // Added by AI Assistant - ChatBot AI API support - 2026-07-26
    val chatApi: ChatApi by lazy {
        instance.create(ChatApi::class.java)
    }

    val newsApi: NewsApi by lazy {
        instance.create(NewsApi::class.java)
    }

    val importOrderApi: ImportOrderApi by lazy {
        instance.create(ImportOrderApi::class.java)
    }
}
