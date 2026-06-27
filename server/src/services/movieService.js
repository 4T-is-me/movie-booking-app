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

    const skip = (Number(page) - 1) * Number(limit);
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
    const movie = await Movie.findOne({ MOVIE_ID: id }).lean();
    return movie;
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Trả về danh sách suất chiếu của phim kèm cinemaRoomName + availableSeats
export const getMovieShowtimes = async (movieId, date) => {
    // 1. Xây dựng filter — movieId bắt buộc, date tuỳ chọn
    const showtimeQuery = { movieId };
    if (date) {
        const target = new Date(date);
        const nextDay = new Date(date);
        nextDay.setDate(nextDay.getDate() + 1);
        showDateQuery.SHOW_DATE = { $gte: target, $lt: nextDay };
    }

    // 2. Lấy danh sách suất chiếu, populate roomId để lấy tên phòng & sức chứa
    const showtimes = await Showtime.find(showtimeQuery)
        .populate('roomId', 'roomName roomType seatCapacity')
        .sort({ showDate: 1, startTime: 1 })
        .lean();

    if (showtimes.length === 0) return null;

    // 3. Đếm ghế đã chiếm (booked + holding còn hiệu lực) cho tất cả suất chiếu
    //    dùng một aggregate duy nhất thay vì N query riêng lẻ
    const showtimeIds = showtimes.map((st) => st._id);
    const now = new Date();

    const occupiedAgg = await ScheduleSeat.aggregate([
        {
            $match: {
                showtimeId: { $in: showtimeIds },
                $or: [
                    { status: 'booked' },
                    // Holding còn hiệu lực (chưa hết hạn)
                    { status: 'holding', holdExpiresAt: { $gt: now } }
                ]
            }
        },
        {
            $group: {
                _id: '$showtimeId',
                occupiedCount: { $sum: 1 }
            }
        }
    ]);

    // Map: showtimeId (string) → số ghế đã chiếm
    const occupiedMap = new Map(
        occupiedAgg.map(({ _id, occupiedCount }) => [_id.toString(), occupiedCount])
    );

    // 4. Gắn availableSeats vào từng suất chiếu
    return showtimes.map((showtime) => {
        const totalSeats = showtime.roomId?.seatCapacity ?? 0;
        const occupied   = occupiedMap.get(showtime._id.toString()) ?? 0;

        return {
            showtimeId:     showtime._id,
            showDate:       showtime.showDate,
            showTime:       showtime.showTime,
            startTime:      showtime.startTime,
            endTime:        showtime.endTime,
            cinemaId:       showtime.cinemaId,
            cinemaRoomName: showtime.roomId?.roomName ?? null,
            room: showtime.roomId
                ? {
                    roomId:       showtime.roomId._id,
                    roomName:     showtime.roomId.roomName,
                    roomType:     showtime.roomId.roomType,
                    seatCapacity: showtime.roomId.seatCapacity
                }
                : null,
            totalSeats,
            availableSeats: Math.max(0, totalSeats - occupied)
        };
    });
};
