import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';
import {Cinema } from '../models/cinemaModel.js';  
import {CinemaRoom } from '../models/cinemaRoomModel.js';
// GET /api/movies
// Hỗ trợ lọc theo trạng thái (now_showing / coming_soon), tìm kiếm tên (q) và phân trang
export const getMovies = async ({ status, page = 1, limit = 10, q }) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const conditions = [];

    // Lọc theo trạng thái chiếu dựa trên releaseDate / endDate / status:
    //   now_showing  → releaseDate <= today AND (endDate >= today OR endDate null)
    //   coming_soon  → status === 'coming_soon' (chưa xác định endDate)
    if (status === 'now_showing') {
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);
        conditions.push({
            releaseDate: { $lte: today },
            $or: [
                { endDate: { $gte: today } },
                { endDate: null },
                { endDate: { $exists: false } }
            ]
        });
    } else if (status === 'coming_soon') {
        conditions.push({ status: 'coming_soon' });
    }

    // Tìm kiếm theo tên phim (movieName)
    if (q && q.trim() !== '') {
        conditions.push({
            movieName: { $regex: q.trim(), $options: 'i' }
        });
    }

    const query = conditions.length > 0 ? { $and: conditions } : {};

    const skip = (Number(page) - 1) * Number(limit);
    const total = await Movie.countDocuments(query);
    const movies = await Movie.find(query)
        .sort({ releaseDate: 1 })
        .skip(skip)
        .limit(Number(limit))
        .lean();

    return {
        page: Number(page),
        limit: Number(limit),
        total,
        totalPages: Math.ceil(total / Number(limit)),
        data: movies
    };
};


// GET /api/movies/search?q=...
// Tìm kiếm theo movieName (dùng $regex + $options 'i'), có phân trang
export const searchMovies = async ({ q, page = 1, limit = 10 }) => {
    if (!q || q.trim() === '') {
        return {
            page: Number(page),
            limit: Number(limit),
            total: 0,
            totalPages: 0,
            message: 'Vui lòng nhập từ khóa tìm kiếm.',
            data: []
        };
    }

    const keyword = q.trim();
    const searchQuery = {
        movieName: { $regex: keyword, $options: 'i' }
    };

    const skip  = (Number(page) - 1) * Number(limit);
    const total = await Movie.countDocuments(searchQuery);
    const movies = await Movie.find(searchQuery)
        .sort({ movieName: 1 })
        .skip(skip)
        .limit(Number(limit))
        .lean();

    if (movies.length === 0) {
        return {
            page: Number(page),
            limit: Number(limit),
            total: 0,
            totalPages: 0,
            message: 'Không tìm thấy phim nào khớp với từ khóa.',
            data: []
        };
    }

    return {
        page: Number(page),
        limit: Number(limit),
        total,
        totalPages: Math.ceil(total / Number(limit)),
        data: movies
    };
};

// GET /api/movies/:id
// Lấy thông tin chi tiết một bộ phim
export const getMovieById = async (id) => {
    return await Movie.findById(id).lean();
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Trả về danh sách suất chiếu của ngày đó kèm cinemaRoomName
// Luồng: MOVIE_DATE → SHOW_DATES (lọc date) + MOVIE_SCHEDULE → SCHEDULE
//        → SCHEDULE_SEAT → SEAT → CINEMA_ROOM
export const getMovieShowtimes = async (movieId, date) => {
    // 1. Kiểm tra phim có ngày chiếu không
    const movieDates = await MovieDate.find({ MOVIE_ID: movieId }).lean();
    if (movieDates.length === 0) return null;

    const showDateIds = movieDates.map(md => md.SHOW_DATE_ID);

    // 2. Lọc ngày chiếu theo date (nếu có)
    const showDateQuery = { SHOW_DATE_ID: { $in: showDateIds } };

    if (date) {
        const target = new Date(date);
        const nextDay = new Date(date);
        nextDay.setDate(nextDay.getDate() + 1);
        query.showDate  = { $gte: target, $lt: nextDay };
    }
    const showDates = await ShowDates.find(showDateQuery).sort({ SHOW_DATE: 1 }).lean();
    if (showDates.length === 0) return null;

    // 3. Lấy danh sách SCHEDULE_ID của phim (một lần duy nhất)
    const movieSchedules = await MovieSchedule.find({ MOVIE_ID: movieId }).lean();
    if (movieSchedules.length === 0) return null;
    const scheduleIds = movieSchedules.map(ms => ms.SCHEDULE_ID);

    // Với mỗi ngày chiếu, lấy danh sách giờ chiếu qua MovieSchedule -> Schedule
    const result = showtimes.map((showtime) => ({
            showtimeId:showtime._id,
            showDate: showtime.showDate,
            showTime: showtime.showTime,
            startTime: showtime.startTime,
            endTime: showtime.endTime,
            cinema: showtime.cinemaId?.cinemaName,
            room: showtime.roomId?.roomName
        })
    );

    return result;
};
