import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Seat } from '../models/seatModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';
import { Showtime } from '../models/showTimeModel.js';
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
            return 'Hold';
    }
};

/**
 * GET /api/showtimes/:id/seats
 * Returns the room seat map for a showtime with merged booking/holding status.
 */
export const getShowtimeSeats = async (showtimeId) => {
    const showtime = await Showtime.findById(showtimeId).lean();
    if (!showtime) return null;

    const [cinemaRoom, seats, scheduleSeats] = await Promise.all([
        CinemaRoom.findById(showtime.roomId).lean(),
        Seat.find({ roomId: showtime.roomId, status: 'active' }).sort({ row: 1, col: 1 }).lean(),
        ScheduleSeat.find({ showtimeId }).lean()
    ]);

    const scheduleSeatMap = new Map(
        scheduleSeats.map((scheduleSeat) => [
            scheduleSeat.seatId.toString(),
            scheduleSeat
        ])
    );

    const seatList = seats.map((seat) => {
        const scheduleSeat = scheduleSeatMap.get(seat._id.toString());
        const status = getSeatStatus(scheduleSeat);

        return {
            seatId: seat._id,
            seatName: seat.seatName,
            row: seat.row,
            col: seat.col,
            seatType: seat.seatType,
            price: seat.price,
            status,
            statusLabel: getSeatStatusLabel(status),
            holdExpiresAt: scheduleSeat?.status === 'holding' ? scheduleSeat.holdExpiresAt : null,

            // Backward-compatible aliases for older seat-map clients.
            seat_id: seat._id,
            seat_row: seat.row,
            seat_column: seat.col,
            seat_type: seat.seatType,
            seat_status: status,
            seat_status_label: getSeatStatusLabel(status)
        };
    });

    return {
        showtimeId: showtime._id,
        showDate: showtime.showDate,
        showTime: showtime.showTime,
        startTime: showtime.startTime,
        endTime: showtime.endTime,
        movieId: showtime.movieId,
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
        seats: seatList,

        // Backward-compatible aliases for older clients.
        showtimeId: showtime._id,
        showtime: showtime.showTime,
        movieId: showtime.movieId,
        cinema_room: cinemaRoom
            ? {
                roomId: cinemaRoom._id,
                roomName: cinemaRoom.roomName,
                seatCapacity: cinemaRoom.seatCapacity
            }
            : null
    };
};
