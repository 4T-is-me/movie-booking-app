package com.example.movie_booking_app.utils

object Constants {
    /**
     * GLOBAL SWITCH:
     * true -> Use Mock Data (Stable for UI development according to Figma)
     * false -> Use Real Server/DB (For testing connectivity and merging code)
     */
    const val useMockData = false

    /**
     * Updated to your machine's actual IPv4: 192.168.1.184
     * Synced with group's server requirements.
     */
    const val BASE_URL = "https://movie-booking-app-6kcy.onrender.com/"
    const val RANK_GOLD = 10000000L
    const val RANK_SILVER = 5000000L

    const val MORNING = "08:00AM - 12:00PM"
    const val AFTERNOON = "12:00PM - 04:00PM"
    const val EVENING = "04:00PM - 08:00PM"
    const val NIGHT = "08:00PM - 12:00AM"
    const val FULLTIME = "08h–22h"
    const val MAX_SHIFT_OF_DATE = 4
    val SHIFTS = listOf("Morning", "Afternoon", "Evening", "Night")

    val STATUS_SHIFT = listOf("Active","Assigned", "Completed", "Absent", "NeedReplacement")

    val STATUS_REQUEST = listOf("pending", "approved", "rejected")

    val  ERROR_DUPLICATE_SHIFT =  "ERROR_DUPLICATE_SHIFT"
    val ERROR_UNKNOWN = "ERROR_UNKNOWN"
    object ComboPrice {
        const val SINGLE_COMBO = 80_000
        const val DOUBLE_COMBO = 120_000
    }

    val STATUS_PAYROLL = listOf("All Status", "Draft", "Paid")
    val IMPORT_ORDER_STATUS = listOf("PENDING", "APPROVED", "REJECTED","CANCELLED" )
    val STATUS_OPTIONS = listOf("All", "Draft", "Paid")
    val TIME = listOf("day", "week", "month", "year")
    const val LIMIT_TITLE = 30
    const val CURRENT_TAB = "staff_list"
    const val LEAVE_REQUEST_TAB = "leave_requests"
    const val  LEAVE_REQUEST_CREATED = "LEAVE_REQUEST_CREATED"
    const val  SHIFT_CHANGE_ACCEPTED =  "SHIFT_CHANGE_ACCEPTED"
    const val TARGET_TAB = "target_tab"
    const val UNKNOWN = "Unknown"
    const val UNRATED =  "Unrated"

    object Movie {
        const val EXTRA_MOVIE_DATA = "movie"
        const val MIME_TYPE_IMAGE = "image/*"
        const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
        const val DATE_FORMAT_SERVER = "yyyy-MM-dd"
        const val PREFIX_POSTER_FILE = "movie_poster_"
        const val SUFFIX_JPG = ".jpg"
        const val STATUS_ACTIVE = "active"
        const val STATUS_DELETED = "deleted"

        const val EXTRA_MOVIE_ID = "movieId"
        const val EXTRA_MOVIE_TITLE = "movieTitle"
        const val EXTRA_MOVIE_POSTER = "moviePoster"
        const val EXTRA_MOVIE_GENRE = "movieGenre"

        const val MIN_COMMENT_LENGTH = 5
        const val MAX_COMMENT_LENGTH = 1000

        const val SEPARATOR_DASH = "-"
        const val SEPARATOR_SLASH = "/"
    }

    object MovieStatus {
        const val ACTIVE = "active"
        const val INACTIVE = "inactive"
        const val DELETED = "deleted"
        const val SHOWING = "showing"
        const val COMING_SOON = "coming"
        const val SOON = "soon"
        const val UPCOMING = "upcoming"
        const val COMING_SOON_F = "coming-soon"
        const val NOW_SHOWING = "now_showing"
    }

    object SeatConstants {
        const val TYPE_REGULAR = "Regular"
        const val TYPE_VIP = "VIP"
        const val TYPE_COUPLE = "Couple"
    }

    object BookingConfig {
        const val HOLD_TIME_SECONDS = 600L
        const val DEBOUNCE_SEARCH_MS = 500L
        const val DEFAULT_MOVIE_DURATION = 120
    }

    object FoodCategory {
        const val ALL = "All"
        const val COMBO = "combo"
        const val DRINK = "drink"
        const val FOOD = "food"
    }
    object ShiftPair{
        const val MORNING_AFTERNOON = "Morning - Afternoon"
        const val EVENING_NIGHT = "Evening - Night"
    }

    object SeatStatus {
        const val ACTIVE = "active"
        const val MAINTENANCE = "maintenance"
    }

    object Status {
        const val ACTIVE = "Active"
        const val INACTIVE = "Inactive"
        const val MAINTENANCE = "Maintenance"
        const val DELETE = "Deleted"
        const val PAID = "Paid"
        const val DRAFT = "Draft"
    }
    val GENRE_OPTIONS = arrayOf(
        "Action", "Comedy", "Drama", "Horror", "Romance",
        "Sci-Fi", "Thriller", "Animation", "Adventure", "Fantasy"
    )
    val GENDER_OPTIONS = arrayOf("Male", "Female", "Other")

    val CINEMA_OPTIONS = arrayOf(
        "CGV Vincom Center",
        "BHD Star 3/2",
        "Lotte Cinema Cantavil",
        "Galaxy Nguyen Du",
        "Beta Cinema"
    )
    object NAV {
        const val HOME = "Home"
        const val TICKET = "Ticket"
        const val MOVIE = "Movie"
        const val PROFILE = "Profile"
        const val NOW_PLAYING = "Now Playing"
        const val COMING_SOON = "Coming Soon"
    }

    object PaymentMethod {
        const val MOMO = "MoMo"
        const val ZALO_PAY = "ZaloPay"
        const val SHOPEE_PAY = "ShopeePay"
        const val ATM = "ATM"
        const val INTERNATIONAL = "International"
    }

    object Rank {
        const val GOLD = "gold"
        const val SILVER = "silver"

        const val GOLD_TEXT = "GOLD MEMBER"
        const val SILVER_TEXT = "SILVER MEMBER"
        const val BRONZE_TEXT = "BRONZE MEMBER"

        const val MAX_RANK_RATIO = "MAX RANK"
        const val PROGRESS_MAX_LABEL = "You are at maximum tier"
        const val NEXT_TIER_GOLD_THANK = "Thank you for being our Gold Member!"

        const val PROGRESS_TO_GOLD = "Progress to Gold Member"
        const val NEXT_TIER_GOLD_HINT = "%,d more to reach Gold Member"

        const val PROGRESS_TO_SILVER = "Progress to Silver Member"
        const val NEXT_TIER_SILVER_HINT = "%,d more to reach Silver Member"
    }

    object ErrorCode {
        const val NO_SHOWTIMES = "error_no_showtimes"
        const val NO_SEATS = "error_no_seats"
        const val SAME_TYPE = "error_same_type"
        const val HOLD_CONFLICT = "error_hold_conflict"
        const val HOLD_FAILED = "error_hold_failed"
    }
    object NotificationType {
        const val SHIFT_ASSIGNED = "SHIFT_ASSIGNED"
        const val SHIFT_UPDATED = "SHIFT_UPDATED"
        const val LEAVE_APPROVED = "LEAVE_APPROVED"
        const val LEAVE_REJECTED = "LEAVE_REJECTED"
        const val LEAVE_CANCELLED = "LEAVE_CANCELLED"

        const val PREFIX_LEAVE = "LEAVE_"
        const val PREFIX_SHIFT = "SHIFT_"
    }

    object ImportOrderStatus {
        const val PENDING = "PENDING"
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
        const val CANCELLED = "CANCELLED"

        const val DISPLAY_PENDING = "Pending"
        const val DISPLAY_APPROVED = "Approved"
        const val DISPLAY_REJECTED = "Rejected"
        const val DISPLAY_CANCELLED = "Cancelled"
    }
    object CinemaRoomStatus {
        const val ACTIVE = "active"
        const val OPERATING = "operating"
        const val OPEN = "open"
        const val MAINTENANCE = "maintenance"
        const val MAINTAIN = "maintain"

        const val DISPLAY_ACTIVE = "Active"
        const val DISPLAY_MAINTENANCE = "Maintenance"
        const val DISPLAY_CLOSED = "Closed"

        const val MENU_EDIT = "Edit Room"
        const val MENU_CLOSE = "Close Room"
        const val MENU_CLOSE_CINEMA = "Close Cinema"
        const val MENU_REACTIVATE = "Reactivate Room"
        const val MENU_REACTIVATE_CINEMA = "Reactivate Cinema"

        const val DEFAULT_ROOM_TYPE = "Regular"
        const val DEFAULT_SCREEN_TYPE = "2D"
        const val DEFAULT_ROW = "8"
        const val DEFAULT_COLUMN = "10"
    }

    object CommentReaction {
        const val LIKE = "like"
        const val LOVE = "love"
        const val HAHA = "haha"
        const val WOW = "wow"
        const val SAD = "sad"
        const val ANGRY = "angry"

        const val EMOJI_LIKE = "👍"
        const val EMOJI_LOVE = "❤️"
        const val EMOJI_HAHA = "😆"
        const val EMOJI_WOW = "😲"
        const val EMOJI_SAD = "😢"
        const val EMOJI_ANGRY = "😡"

        const val PREFIX_HTTP = "http"
    }

    object LeaveStatus {
        const val PENDING = "Pending"
        const val APPROVED = "Approved"
        const val REJECTED = "Rejected"

        const val DEFAULT_NO_FEEDBACK = "No feedback provided."
    }
    object MovieActionMessage {
        const val DELETED_SUCCESS = "msg_movie_deleted_success"
        const val RESTORED_SUCCESS = "msg_movie_restored_success"
        const val DELETE_FAILED = "msg_movie_delete_failed"
        const val RESTORE_FAILED = "msg_movie_restore_failed"
        const val ADDED_SUCCESS = "msg_movie_added_success"
        const val ADD_FAILED = "msg_movie_add_failed"
        const val UPDATED_SUCCESS = "msg_movie_updated_success"
        const val UPDATE_FAILED = "msg_movie_update_failed"
        const val ERROR_UNKNOWN = "error_unknown"
    }
    object TAB{
        const val TAB_ACTIVE = "active"
        const val TAB_INACTIVE = "inactive"
    }
    object ROLE{
        const val MANAGER = "Manager"
        const val USER = "User"
        const val ADMIN = "Admin"
        const val STAFF = "Staff"
        const val EMPLOYEE = "employee"
    }

    object SECTION_NAMES {
        const val  PROMO = "Promo"
        const val  SERVICE = "Service"
        const val NEWS = "News"
    }

    val ROOM_TYPES = arrayOf("Standard", "VIP", "IMAX")
    val SCREEN_TYPES =  arrayOf("2D", "3D", "IMAX")
    val CENSORSHIPCODES = arrayOf("P", "K", "T13", "T16", "T18", "C")
    val CATEGORIES = listOf("drink", "food", "combo")
    val COUNT_DOWN  = "10:00"

}



