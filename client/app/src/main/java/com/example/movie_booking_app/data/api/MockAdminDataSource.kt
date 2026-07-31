package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.data.model.admin.StatsOverview
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.CinemaRoomListResponse
import com.example.movie_booking_app.data.model.response.DailyRevenueResponse
import com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse
import com.example.movie_booking_app.data.model.response.MovieRevenueResponse
import com.example.movie_booking_app.data.model.response.OverviewStatsResponse
import com.example.movie_booking_app.data.model.response.PromotionData
import com.example.movie_booking_app.data.model.response.ShowtimeResponse
import com.example.movie_booking_app.data.model.response.UserProfileResponse

/**
 * Mock data source for Admin related entities.
 * (Inherited from ISSUE_27_TEMP - Kien)
 */
object MockAdminDataSource {

    fun getMockOverviewStats(): OverviewStatsResponse {
        return OverviewStatsResponse(
            data = StatsOverview(
                totalRevenue = 54200.0,
                totalMovies = 45,
                totalMembers = 1250,
                totalTicketsSold = 3840
            )
        )
    }

    fun getMockDailyRevenue(): DailyRevenueResponse {
        return DailyRevenueResponse(
            data = listOf(
                DailyRevenueItem("2024-05-20", 1200.0),
                DailyRevenueItem("2024-05-21", 1500.0),
                DailyRevenueItem("2024-05-22", 800.0),
                DailyRevenueItem("2024-05-23", 2100.0),
                DailyRevenueItem("2024-05-24", 1900.0),
                DailyRevenueItem("2024-05-25", 2500.0),
                DailyRevenueItem("2024-05-26", 2800.0)
            )
        )
    }

    fun getMockMovieRevenue(): MovieRevenueResponse {
        return MovieRevenueResponse(
            data = listOf(
                MovieRevenueItem("1", "Avengers: Endgame", "Avengers: Endgame", 15000.0),
                MovieRevenueItem("2", "Spider-Man: No Way Home", "Spider-Man: No Way Home", 12500.0),
                MovieRevenueItem("3", "Jurassic World", "Jurassic World", 9000.0),
                MovieRevenueItem("4", "Avatar: The Way of Water", "Avatar: The Way of Water", 8500.0),
                MovieRevenueItem("5", "The Lion King", "The Lion King", 7200.0)
            )
        )
    }

    fun getMockMonthlyRevenue(): MonthlyRevenueResponse {
        return MonthlyRevenueResponse(
            data = listOf(
                MonthlyRevenueItem(2024, 1, 12000.0, 500),
                MonthlyRevenueItem(2024, 2, 15000.0, 650),
                MonthlyRevenueItem(2024, 3, 11000.0, 480),
                MonthlyRevenueItem(2024, 4, 18000.0, 720),
                MonthlyRevenueItem(2024, 5, 22000.0, 900),
                MonthlyRevenueItem(2024, 6, 19000.0, 800),
                MonthlyRevenueItem(2024, 7, 25000.0, 1100),
                MonthlyRevenueItem(2024, 8, 28000.0, 1200),
                MonthlyRevenueItem(2024, 9, 21000.0, 850),
                MonthlyRevenueItem(2024, 10, 23000.0, 950),
                MonthlyRevenueItem(2024, 11, 26000.0, 1050),
                MonthlyRevenueItem(2024, 12, 30000.0, 1300)
            )
        )
    }

//    fun getMockMovies(): MovieListResponse {
//        return MovieListResponse(
//            page = 1,
//            limit = 10,
//            total = 2,
//            totalPages = 1,
//            data = listOf(
//                Movie(
//                    id = "1",
//                    movieName = "Avengers: Endgame",
//                    duration = 181,
//                    releaseDate = "2019-04-26",
//                    movieGenre = listOf("Action", "Sci-Fi"),
//                    language = "English",
//                    censorship = "PG-13",
//                    description = "After the devastating events of Infinity War...",
//                    director = listOf("Anthony Russo", "Joe Russo"),
//                    actors = listOf("Robert Downey Jr.", "Chris Evans"),
//                    country = "USA",
//                    status = "released",
//                    poster = "https://link-to-poster.com/endgame.jpg",
//                    rating = 8.4,
//                    totalReviews = 1000000,
//                    createdAt = "2024-01-01",
//                    updatedAt = "2024-01-01",
//                    totalStars = 5,
//                    isDeleted = false
//                ),
//                Movie(
//                    id = "2",
//                    movieName = "Old Movie",
//                    duration = 120,
//                    releaseDate = "2010-01-01",
//                    movieGenre = listOf("Drama"),
//                    language = "English",
//                    censorship = "PG",
//                    description = "A very old movie description.",
//                    director = listOf("Unknown"),
//                    actors = listOf("Unknown"),
//                    country = "USA",
//                    status = "archived",
//                    poster = null,
//                    rating = 6.0,
//                    totalReviews = 100,
//                    createdAt = "2024-01-01",
//                    updatedAt = "2024-01-01",
//                    totalStars = 3,
//                    isDeleted = true
//                )
//            )
//        )
//    }

    fun getMockRooms(): CinemaRoomListResponse {
        return CinemaRoomListResponse(
            status = "success",
            data = listOf(
                CinemaRoom("room1", "Room A", 100, 10, 10),
                CinemaRoom("room2", "Room B", 80, 8, 10)
            )
        )
    }

    fun getMockShowtimes(): ShowtimeResponse {
        return ShowtimeResponse(
            data = listOf(
                Showtime("st1", "1", null, "room1", "2024-06-01", "10:00", "13:00"),
                Showtime("st2", "1", null, "room1", "2024-06-01", "14:00", "17:00")
            )
        )
    }

    fun getMockEmployees(): List<UserProfileResponse> {
        return listOf(
            UserProfileResponse(
                username = "admin1",
                fullName = "Admin User",
                email = "admin@movie.com",
                phone = "0123456789",
                address = "Hanoi",
                role = "admin"
            ),
            UserProfileResponse(
                username = "staff1",
                fullName = "Staff User",
                email = "staff@movie.com",
                phone = "0987654321",
                address = "HCM",
                role = "employee"
            )
        )
    }

    fun getMockPromotions(): List<PromotionData> {
        return listOf(
            PromotionData("p1", "Summer Sale", "Get 20% off", 20),
            PromotionData("p2", "New User", "Welcome discount", 10)
        )
    }
}
