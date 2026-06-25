import { Movie } from '../models/movieModel.js';

// GET /api/movies
// Hỗ trợ lọc theo trạng thái (now_showing / coming_soon) và phân trang
export const getMovies = async ({ status, page = 1, limit = 10 }) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const query = {};
    if (status === 'now_showing') {
        query.FROM_DATE = { $lte: today };
        query.TO_DATE = { $gte: today };
    } else if (status === 'coming_soon') {
        query.FROM_DATE = { $gt: today };
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
        $or: [
            { MOVIE_NAME_VN: { $regex: regex } },
            { MOVIE_NAME_ENGLISH: { $regex: regex } }
        ]
    })
        .sort({ MOVIE_NAME_VN: 1 })
        .lean();

    if (movies.length === 0) {
        return { message: 'Không tìm thấy phim nào khớp với từ khóa.', data: [] };
    }

    return { data: movies };
};

// GET /api/movies/:id
// Lấy thông tin chi tiết một bộ phim
export const getMovieById = async (id) => {
    const movie = await Movie.findOne({ MOVIE_ID: id }).lean();
    return movie;
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Lấy danh sách ngày chiếu và khung giờ chiếu của phim
export const getMovieShowtimes = async (movieId, date) => {
    // Lấy tất cả SHOW_DATE_ID của phim này
    const movieDates = await MovieDate.find({ MOVIE_ID: movieId }).lean();
    if (movieDates.length === 0) return null;

    const showDateIds = movieDates.map(md => md.SHOW_DATE_ID);

    // Lọc ShowDates theo date nếu có
    const showDateQuery = { SHOW_DATE_ID: { $in: showDateIds } };
    if (date) {
        const target = new Date(date);
        const nextDay = new Date(date);
        nextDay.setDate(nextDay.getDate() + 1);
        showDateQuery.SHOW_DATE = { $gte: target, $lt: nextDay };
    }

    const showDates = await ShowDates.find(showDateQuery).sort({ SHOW_DATE: 1 }).lean();

    // Với mỗi ngày chiếu, lấy danh sách giờ chiếu qua MovieSchedule -> Schedule
    const result = await Promise.all(
        showDates.map(async (sd) => {
            const movieSchedules = await MovieSchedule.find({ MOVIE_ID: movieId }).lean();
            const scheduleIds = movieSchedules.map(ms => ms.SCHEDULE_ID);
            const schedules = await Schedule.find({ SCHEDULE_ID: { $in: scheduleIds } })
                .sort({ SCHEDULE_TIME: 1 })
                .lean();

            return {
                show_date_id: sd.SHOW_DATE_ID,
                show_date: sd.SHOW_DATE,
                date_name: sd.DATE_NAME,
                schedules: schedules.map(s => ({
                    schedule_id: s.SCHEDULE_ID,
                    schedule_time: s.SCHEDULE_TIME
                }))
            };
        })
    );

    return result;
};
