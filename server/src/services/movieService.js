import { Movie } from '../models/movieModel.js';


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

    // 4. Load Schedule và ScheduleSeat song song (tránh N+1)
    const [schedules, scheduleSeats] = await Promise.all([
        Schedule.find({ SCHEDULE_ID: { $in: scheduleIds } })
            .sort({ SCHEDULE_TIME: 1 })
            .lean(),
        ScheduleSeat.find({ MOVIE_ID: movieId, SCHEDULE_ID: { $in: scheduleIds } })
            .lean()
    ]);

    // 5. Resolve CINEMA_ROOM qua SEAT (tránh N+1)
    //    Mỗi SCHEDULE_SEAT có SEAT_ID → SEAT.CINEMA_ROOM_ID → CINEMA_ROOM
    const seatIds = [...new Set(scheduleSeats.map(ss => ss.SEAT_ID))];
    const seats = await Seat.find({ SEAT_ID: { $in: seatIds } }).lean();
    const seatMap = Object.fromEntries(seats.map(s => [s.SEAT_ID, s]));

    const roomIds = [...new Set(seats.map(s => s.CINEMA_ROOM_ID))];
    const rooms = await CinemaRoom.find({ CINEMA_ROOM_ID: { $in: roomIds } }).lean();
    const roomMap = Object.fromEntries(rooms.map(r => [r.CINEMA_ROOM_ID, r]));

    // 6. Map scheduleId → cinemaRoomName (dùng bản ghi ScheduleSeat đầu tiên của schedule)
    const scheduleCinemaRoom = {};
    for (const ss of scheduleSeats) {
        if (scheduleCinemaRoom[ss.SCHEDULE_ID]) continue; // chỉ cần 1 bản ghi
        const seat = seatMap[ss.SEAT_ID];
        if (!seat) continue;
        const room = roomMap[seat.CINEMA_ROOM_ID];
        if (room) {
            scheduleCinemaRoom[ss.SCHEDULE_ID] = {
                cinema_room_id: room.CINEMA_ROOM_ID,
                cinema_room_name: room.CINEMA_ROOM_NAME
            };
        }
    }

    // 7. Ghép kết quả theo từng ngày chiếu
    const result = showDates.map(sd => ({
        show_date_id: sd.SHOW_DATE_ID,
        show_date: sd.SHOW_DATE,
        date_name: sd.DATE_NAME,
        showtimes: schedules.map(s => ({
            schedule_id: s.SCHEDULE_ID,
            schedule_time: s.SCHEDULE_TIME,
            cinema_room: scheduleCinemaRoom[s.SCHEDULE_ID] || null
        }))
    }));

    return result;
};
