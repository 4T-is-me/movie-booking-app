package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.api.AuthApi
import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.api.ChatApi
import com.example.movie_booking_app.data.api.CinemaRoomApi
import com.example.movie_booking_app.data.api.FcmApi
import com.example.movie_booking_app.data.api.FoodApi
import com.example.movie_booking_app.data.api.ImportOrderApi
import com.example.movie_booking_app.data.api.InventoryApi
import com.example.movie_booking_app.data.api.LeaveRequestApi
import com.example.movie_booking_app.data.api.MaintenanceApi
import com.example.movie_booking_app.data.api.ManagerApi
import com.example.movie_booking_app.data.api.ManagerRoomApi
import com.example.movie_booking_app.data.api.MemberApi
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.api.NewsApi
import com.example.movie_booking_app.data.api.NotificationApi
import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.api.PayrollApi
import com.example.movie_booking_app.data.api.PayrollConfigApi
import com.example.movie_booking_app.data.api.ProfileApi
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.api.StaffApi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMovieApi(): MovieApi {
        return RetrofitClient.movieApi
    }

    @Provides
    @Singleton
    fun provideCinemaRoomApi(): CinemaRoomApi {
        return RetrofitClient.cinemaRoomApi
    }

    @Provides
    @Singleton
    fun provideBookingApi(): BookingApi {
        return RetrofitClient.bookingApi
    }

    @Provides
    @Singleton
    fun provideAuthApi(): AuthApi {
        return RetrofitClient.authApi
    }

    @Provides
    @Singleton
    fun provideProfileApi(): ProfileApi {
        return RetrofitClient.profileApi
    }

    @Provides
    @Singleton
    fun providePaymentApi(): PaymentApi {
        return RetrofitClient.paymentApi
    }

    @Provides
    @Singleton
    fun provideAdminApi(): AdminApiService {
        return RetrofitClient.adminApi
    }

    @Provides
    @Singleton
    fun provideManagerRoomApi(): ManagerRoomApi {
        return RetrofitClient.managerRoomApi
    }

    @Provides
    @Singleton
    fun provideManagerApi(): ManagerApi {
        return RetrofitClient.managerApi
    }

    @Provides
    @Singleton
    fun provideFcmApi(): FcmApi {
        return RetrofitClient.fcmApi
    }

    @Provides
    @Singleton
    fun provideMemberApi(): MemberApi {
        return RetrofitClient.memberApi
    }

    @Provides
    @Singleton
    fun provideStaffApi(): StaffApi {
        return RetrofitClient.staffApi
    }
    
    @Provides
    @Singleton
    fun provideLeaveRequestApi(): LeaveRequestApi {
        return RetrofitClient.leaveRequestApi
    }

    @Provides
    @Singleton
    fun provideNotificationApi(): NotificationApi {
        return RetrofitClient.notificationApi
    }

    @Provides
    @Singleton
    fun provideFoodApi(): FoodApi {
        return RetrofitClient.foodApi
    }

    @Provides
    @Singleton
    fun provideInventoryApi(): InventoryApi {
        return RetrofitClient.inventoryApi
    }

    @Provides
    @Singleton
    fun providePayrollApi(): PayrollApi {
        return RetrofitClient.payrollApi
    }

    @Provides
    @Singleton
    fun providePayrollConfigApi(): PayrollConfigApi = RetrofitClient.payrollConfigApi

    @Provides
    @Singleton
    fun provideChatApi(): ChatApi {
        return RetrofitClient.chatApi
    }

    @Provides
    @Singleton
    fun provideNewsApi(): NewsApi {
        return RetrofitClient.newsApi
    }

    @Provides
    @Singleton
    fun provideMaintenanceApi(): MaintenanceApi {
        return RetrofitClient.maintenanceApi
    }

    @Provides
    @Singleton
    fun provideImportOrderApi(): ImportOrderApi {
        return RetrofitClient.importOrderApi
    }
}
