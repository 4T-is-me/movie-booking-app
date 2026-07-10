import { Showtime } from '../models/showTimeModel.js';
import { Seat } from '../models/seatModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';
import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Movie } from '../models/movieModel.js';

export const createShowtime = async (showtimeData) => {
    const { movieId, cinemaId, roomId, showDate, showTime, ticketPrice } = showtimeData;

    // 1. Check conflict showdate and showtime in same room
    const conflict = await Showtime.findOne({
        roomId,
        showDate,
        showTime,
        isDeleted: false
    });

    if (conflict) {
        throw new Error('CONFLICT');
    }

    const movie = await Movie.findById(movieId);
    if (!movie) {
        throw new Error('MovieNotFound');
    }

    const [hours, minutes] = showTime.split(':').map(Number);
    const startTime = new Date(showDate);
    startTime.setUTCHours(hours - 7, minutes, 0, 0); // Convert local +07:00 time to UTC

    const durationMinutes = movie.duration || 120;
    const endTime = new Date(startTime.getTime() + durationMinutes * 60 * 1000);

    // 2. Create showtime document
    const showtime = new Showtime({
        movieId,
        roomId,
        cinemaId,
        showDate,
        showTime,
        ticketPrice,
        startTime,
        endTime,
        isDeleted: false
    });
    const savedShowtime = await showtime.save();

    // 3. Query all seat in room
    const seats = await Seat.find({ roomId });

    // 4. Create all schedule seat with status=1 (empty)
    const scheduleSeats = seats.map(seat => ({
        showtimeId: savedShowtime._id,
        seatId: seat._id,
        status: 1 // status=1 (empty) as requested
    }));

    if (scheduleSeats.length > 0) {
        await ScheduleSeat.insertMany(scheduleSeats);
    }

    return savedShowtime;
};

export const getShowtimesAdmin = async ({ movieId, cinemaId, showDate, page = 1, limit = 20 }) => {
    const filter = { isDeleted: false };
    if (movieId) filter.movieId = movieId;
    if (cinemaId) filter.cinemaId = cinemaId;
    if (showDate) {
        const target = new Date(showDate);
        const nextDay = new Date(showDate);
        nextDay.setDate(nextDay.getDate() + 1);
        filter.showDate = { $gte: target, $lt: nextDay };
    }

    const skip = (page - 1) * limit;
    const total = await Showtime.countDocuments(filter);
    const showtimes = await Showtime.find(filter)
        .populate('movieId', 'movieName poster')
        .populate('cinemaId', 'cinemaName')
        .populate('roomId', 'roomName')
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .lean();

    return {
        page: Number(page),
        limit: Number(limit),
        total,
        totalPages: Math.ceil(total / limit),
        data: showtimes
    };
};

export const getShowtimeDetail = async (id) => {
    return await Showtime.findOne({ _id: id, isDeleted: false })
        .populate('movieId')
        .populate('cinemaId')
        .populate('roomId')
        .lean();
};

export const softDeleteShowtime = async (id) => {
    const showtime = await Showtime.findById(id);
    if (!showtime) return null;
    if (showtime.isDeleted) throw new Error('ALREADY_DELETED');

    showtime.isDeleted = true;
    showtime.deletedAt = new Date();
    await showtime.save();

    return showtime;
};
