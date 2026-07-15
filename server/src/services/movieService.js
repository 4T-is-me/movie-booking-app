import { Movie } from '../models/movieModel.js';
import { getShowtimeTimeRange } from '../utils/formatters.js';

// GET /api/movies
// Support filtering by status (now_showing / coming_soon), search by name (q), and pagination
export const getMovies = async ({ status, page = 1, limit = 10, q }) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const conditions = [{ isDeleted: false }];

    // Search by movie name (movieName)
    if (q && q.trim() !== '') {
        conditions.push({
            movieName: { $regex: q.trim(), $options: 'i' }
        });
    }

    // Filter by screening status based on releaseDate / endDate / status:
    //   now_showing  → releaseDate <= today AND (endDate >= today OR endDate is null)
    //   coming_soon  → status === 'coming_soon' (endDate not set)
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

    // Search by movie name (movieName)
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
// Search by movieName (using $regex + $options 'i'), with pagination
export const searchMovies = async ({ q, page = 1, limit = 10 }) => {
    if (!q || q.trim() === '') {
        return {
            page: Number(page),
            limit: Number(limit),
            total: 0,
            totalPages: 0,
            message: 'Please enter search keyword.',
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
            message: 'No movies found matching the keyword.',
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
// Get movie details
export const getMovieById = async (id) => {
    const movie = await Movie.findOne({ _id: id, isDeleted: false }).lean();
    return movie;
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Returns list of movie showtimes with cinemaRoomName + availableSeats
export const getMovieShowtimes = async (movieId, date) => {
    // 1. Build filter - movieId is required, date is optional
    const showtimeQuery = { movieId };
    if (date) {
        const target = new Date(date);
        const nextDay = new Date(date);
        nextDay.setDate(nextDay.getDate() + 1);
        showDateQuery.SHOW_DATE = { $gte: target, $lt: nextDay };
    }

    // 2. Get list of showtimes, populate roomId to get room name & seat capacity
    const showtimes = await Showtime.find(showtimeQuery)
        .populate('roomId', 'roomName roomType seatCapacity')
        .populate('cinemaId', 'cinemaName')
        .sort({ showDate: 1, startTime: 1 })
        .lean();

    if (showtimes.length === 0) return null;

    // 3. Count occupied seats (booked + active holding) for all showtimes
    //    using a single aggregation instead of N separate queries
    const showtimeIds = showtimes.map((st) => st._id);
    const now = new Date();

    const occupiedAgg = await ScheduleSeat.aggregate([
        {
            $match: {
                showtimeId: { $in: showtimeIds },
                $or: [
                    { status: 'booked' },
                    // Active holding (not expired)
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

    // Map: showtimeId (string) → number of occupied seats
    const occupiedMap = new Map(
        occupiedAgg.map(({ _id, occupiedCount }) => [_id.toString(), occupiedCount])
    );

    // 4. Attach availableSeats to each showtime
    return showtimes.map((showtime) => {
        const totalSeats = showtime.roomId?.seatCapacity ?? 0;
        const occupied   = occupiedMap.get(showtime._id.toString()) ?? 0;

        return {
            showtimeId:     showtime._id,
            showDate:       showtime.showDate,
            showTime:       showtime.showTime || getShowtimeTimeRange(showtime),
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
