import { Movie } from '../models/movieModel.js';
import { MovieDate } from '../models/movieDateModel.js';
import { ShowDates } from '../models/showDatesModel.js';
import { MovieSchedule } from '../models/movieScheduleModel.js';
import { Schedule } from '../models/scheduleModel.js';
import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Seat } from '../models/seatModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';

// GET /api/movies
// Hỗ trợ lọc theo trạng thái (now_showing / coming_soon), tìm kiếm tên (q) và phân trang
export const getMovies = async ({ status, page = 1, limit = 10, q }) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const conditions = [];

    // Lọc theo trạng thái chiếu:
    //   now_showing  → FROM_DATE (releaseDate) <= today <= TO_DATE (endDate)
    //   coming_soon  → FROM_DATE (releaseDate) > today  ≡  FROM_DATE >= tomorrow
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (status === 'now_showing') {
        // FROM_DATE <= today  AND  TO_DATE >= today
        conditions.push({
            FROM_DATE: { $lte: today },
            TO_DATE:   { $gte: today }
        });
    } else if (status === 'coming_soon') {
        // FROM_DATE > today  ≡  FROM_DATE >= tomorrow (tránh edge case timestamp)
        conditions.push({ FROM_DATE: { $gte: tomorrow } });
    }

    // Tìm kiếm tương đối theo tên tiếng Việt hoặc tiếng Anh
    if (q && q.trim() !== '') {
        const regex = new RegExp(q.trim(), 'i');
        conditions.push({
            $or: [
                { MOVIE_NAME_VN: { $regex: regex } },
                { MOVIE_NAME_ENGLISH: { $regex: regex } }
            ]
        });
    }

    const query = conditions.length > 0 ? { $and: conditions } : {};

    const skip = (Number(page) - 1) * Number(limit);
    const total = await Movie.countDocuments(query);
    const movies = await Movie.find(query)
        .sort({ FROM_DATE: 1 })
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
// Tìm kiếm tương đối theo tên tiếng Việt hoặc tiếng Anh (dùng $regex string + $options 'i'), có phân trang
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
        $or: [
            { MOVIE_NAME_VN:      { $regex: keyword, $options: 'i' } },
            { MOVIE_NAME_ENGLISH: { $regex: keyword, $options: 'i' } }
        ]
    };

    const skip  = (Number(page) - 1) * Number(limit);
    const total = await Movie.countDocuments(searchQuery);
    const movies = await Movie.find(searchQuery)
        .sort({ MOVIE_NAME_VN: 1 })
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
    const movie = await Movie.findOne({ MOVIE_ID: id }).lean();
    return movie;
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
        showDateQuery.SHOW_DATE = { $gte: target, $lt: nextDay };
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
