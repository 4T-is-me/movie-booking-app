import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Seat } from '../models/seatModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';
import { Showtime } from '../models/showTimeModel.js';
import { Movie } from '../models/movieModel.js';
import { constants } from '../utils/constants.js';

const getSeatStatus = (scheduleSeat) => {
    if (!scheduleSeat) return constants.SEAT_STATUS.AVAILABLE;

    if (scheduleSeat.status === 'booked') {
        return constants.SEAT_STATUS.SOLD;
    }

    if (
        scheduleSeat.status === 'holding' &&
        scheduleSeat.holdExpiresAt &&
        scheduleSeat.holdExpiresAt > new Date()
    ) {
        return constants.SEAT_STATUS.HOLD;
    }

    return constants.SEAT_STATUS.AVAILABLE;
};

const getSeatStatusLabel = (status) => {
    switch (status) {
        case constants.SEAT_STATUS.SOLD:
            return 'Sold';
        case constants.SEAT_STATUS.HOLD:
            return 'Hold';
        default:
            return 'Available';
    }
};

/**
 * GET /api/showtimes/:id/seats
 * Returns the room seat map for a showtime with merged booking/holding status.
 * Seats are grouped by seatRow to serve SeatMapActivity's matrix renderer.
 */
export const getShowtimeSeats = async (showtimeId) => {
    // Populate movieId to get movie info in a single query (no extra API call needed)
    const showtime = await Showtime.findById(showtimeId)
        .populate('movieId', 'movieName poster duration language censorship movieGenre')
        .lean();
    if (!showtime) return null;

    const movie = showtime.movieId; // populated Movie document (or null)

    // Fetch cinema room info in parallel with seat data
    const [cinemaRoom, scheduleSeats, allSeats] = await Promise.all([
        CinemaRoom.findById(showtime.roomId).lean(),
        // Populate seatId to get row, col, seatType directly from Seat collection
        ScheduleSeat.find({ showtimeId })
            .populate('seatId', 'seatName row col seatType price')
            .lean(),
        // Fetch all active seats in the room so unbooked seats also appear in the map
        Seat.find({ roomId: showtime.roomId, status: 'active' })
            .sort({ row: 1, col: 1 })
            .lean()
    ]);

    // Build a map: seatId (string) → scheduleSeat for O(1) status lookup
    const scheduleSeatMap = new Map(
        scheduleSeats
            .filter((ss) => ss.seatId) // guard against orphaned records
            .map((ss) => [ss.seatId._id.toString(), ss])
    );

    // Map every physical seat → enriched seat object
    const seatList = allSeats.map((seat) => {
        const scheduleSeat = scheduleSeatMap.get(seat._id.toString());
        const status = getSeatStatus(scheduleSeat);

        return {
            seatId: seat._id,
            seatName: seat.seatName,
            row: seat.row,         // e.g. "A"
            col: seat.col,         // e.g. "1"
            seatType: seat.seatType,
            price: seat.price,
            status,
            statusLabel: getSeatStatusLabel(status),
            holdExpiresAt: scheduleSeat?.status === 'holding' ? scheduleSeat.holdExpiresAt : null
        };
    });

    // Group seats by row → [{ row: "A", seats: [{ col, type, status, ... }] }, ...]
    const rowMap = new Map();
    for (const seat of seatList) {
        if (!rowMap.has(seat.row)) {
            rowMap.set(seat.row, []);
        }
        rowMap.get(seat.row).push({
            seatId: seat.seatId,
            seatName: seat.seatName,
            col: seat.col,
            type: seat.seatType,
            price: seat.price,
            status: seat.status,
            statusLabel: seat.statusLabel,
            holdExpiresAt: seat.holdExpiresAt
        });
    }

    // Sort rows alphabetically; seats within each row sorted by col (numeric-aware)
    const rows = Array.from(rowMap.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([row, seats]) => ({
            row,
            seats: seats.sort((a, b) =>
                String(a.col).localeCompare(String(b.col), undefined, { numeric: true })
            )
        }));

    return {
        // --- Showtime info ---
        showtimeId: showtime._id,
        showDate: showtime.showDate,
        showTime: showtime.showTime,
        startTime: showtime.startTime,
        endTime: showtime.endTime,

        // --- Movie info (populated) ---
        movieId: movie?._id ?? null,
        movieName: movie?.movieName ?? null,
        moviePoster: movie?.poster ?? null,
        movieDuration: movie?.duration ?? null,
        movieLanguage: movie?.language ?? null,
        movieCensorship: movie?.censorship ?? null,
        movieGenre: movie?.movieGenre ?? [],

        // --- Cinema / Room info ---
        cinemaId: showtime.cinemaId,
        cinemaRoom: cinemaRoom
            ? {
                roomId: cinemaRoom._id,
                roomName: cinemaRoom.roomName,
                roomType: cinemaRoom.roomType,
                totalSeats: cinemaRoom.totalSeats,
                rowsCount: cinemaRoom.rowsCount,
                colsCount: cinemaRoom.colsCount,
                seatCapacity: cinemaRoom.seatCapacity
            }
            : null,
        cinemaRoomName: cinemaRoom?.roomName ?? null,

        // --- Seat map ---
        // Grouped by row — directly serves SeatMapActivity matrix renderer
        rows,
        // Flat list — kept for backward compatibility with existing clients
        seats: seatList
    };
};
