import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';
import {Cinema } from '../models/cinemaModel.js';  
import {CinemaRoom } from '../models/cinemaRoomModel.js';
// GET /api/movies
// Hỗ trợ lọc theo trạng thái (now_showing / coming_soon) và phân trang
export const getMovies = async ({ status, page = 1, limit = 10 }) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const query = {};
    if (status === 'now_showing') {
        query.releaseDate = { $lte: today };
    } else if (status === 'coming_soon') {
        query.releaseDate = { $gt: today };
    }

    const skip = (Number(page) - 1) * Number(limit);
    const total = await Movie.countDocuments(query);
    const movies = await Movie.find(query)
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
// Tìm kiếm tương đối theo tên tiếng Việt hoặc tiếng Anh, sắp xếp a-z
export const searchMovies = async ({ q }) => {
    if (!q || q.trim() === '') {
        return { message: 'Không tìm thấy phim nào khớp với từ khóa.', data: [] };
    }

    const regex = new RegExp(q.trim(), 'i');
    const movies = await Movie.find({
            movieName: {
                $regex: regex
            }
            })
            .sort({
                movieName: 1
            })
            .lean();

    if (movies.length === 0) {
        return { message: 'Không tìm thấy phim nào khớp với từ khóa.', data: [] };
    }

    return { data: movies };
};

// GET /api/movies/:id
// Lấy thông tin chi tiết một bộ phim
export const getMovieById = async (id) => {
    return await Movie.findById(id).lean();
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Lấy danh sách ngày chiếu và khung giờ chiếu của phim
export const getMovieShowtimes = async (movieId, date) => {

    const query = {movieId};
    if (date) {
        const target = new Date(date);
        const nextDay = new Date(date);
        nextDay.setDate(nextDay.getDate() + 1);
        query.showDate  = { $gte: target, $lt: nextDay };
    }

    const showtimes  = await Showtime.find(query).populate("cinemaId").populate("roomId").sort({startTime: 1}).lean();

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
