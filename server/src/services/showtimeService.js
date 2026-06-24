import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Seat } from '../models/seatModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';

/**
 * GET /api/showtimes/:id/seats
 * Trả về cấu trúc phòng chiếu và trạng thái từng vị trí ghế (trống, đã bán, đang chọn)
 * SEAT_STATUS: 0 = trống, 1 = đã bán, 2 = đang chọn
 * SEAT_TYPE:   1 = Thường, 2 = VIP
 */
export const getShowtimeSeats = async (scheduleId) => {
    // Lấy thông tin suất chiếu
    const schedule = await Schedule.findOne({ SCHEDULE_ID: Number(scheduleId) }).lean();
    if (!schedule) return null;

    // Lấy bộ phim đang chiếu trong suất này
    const movieSchedule = await MovieSchedule.findOne({ SCHEDULE_ID: Number(scheduleId) }).lean();

    // Lấy toàn bộ ghế của suất chiếu này từ SCHEDULE_SEAT
    const scheduleSeats = await ScheduleSeat.find({ SCHEDULE_ID: Number(scheduleId) }).lean();

    if (scheduleSeats.length === 0) {
        // Fallback: nếu chưa có dữ liệu SCHEDULE_SEAT, lấy ghế từ SEAT
        // (trường hợp chưa sinh dữ liệu schedule seat)
        const seatIds = scheduleSeats.map(ss => ss.SEAT_ID);
        return {
            schedule_id: schedule.SCHEDULE_ID,
            schedule_time: schedule.SCHEDULE_TIME,
            movie_id: movieSchedule?.MOVIE_ID || null,
            cinema_room: null,
            seats: []
        };
    }

    // Lấy thông tin ghế gốc để có CINEMA_ROOM_ID
    const seatIds = scheduleSeats.map(ss => ss.SEAT_ID);
    const seats = await Seat.find({ SEAT_ID: { $in: seatIds } }).lean();
    const seatMap = {};
    seats.forEach(s => { seatMap[s.SEAT_ID] = s; });

    // Xác định phòng chiếu
    const cinemaRoomId = seats.length > 0 ? seats[0].CINEMA_ROOM_ID : null;
    const cinemaRoom = cinemaRoomId
        ? await CinemaRoom.findOne({ CINEMA_ROOM_ID: cinemaRoomId }).lean()
        : null;

    // Ghép thông tin ghế
    const seatList = scheduleSeats.map(ss => ({
        seat_id: ss.SEAT_ID,
        seat_row: ss.SEAT_ROW,
        seat_column: ss.SEAT_COLUMN,
        seat_type: ss.SEAT_TYPE,        // 1 = Thường, 2 = VIP
        seat_type_label: ss.SEAT_TYPE === 2 ? 'VIP' : 'Thường',
        seat_status: ss.SEAT_STATUS,    // 0 = trống, 1 = đã bán, 2 = đang chọn
        seat_status_label: ss.SEAT_STATUS === 1 ? 'Đã bán'
            : ss.SEAT_STATUS === 2 ? 'Đang chọn'
                : 'Trống'
    }));

    // Sắp xếp ghế theo hàng rồi theo cột
    seatList.sort((a, b) => {
        if (a.seat_row !== b.seat_row) return a.seat_row - b.seat_row;
        return a.seat_column.localeCompare(b.seat_column);
    });

    return {
        schedule_id: schedule.SCHEDULE_ID,
        schedule_time: schedule.SCHEDULE_TIME,
        movie_id: movieSchedule?.MOVIE_ID || null,
        cinema_room: cinemaRoom
            ? {
                cinema_room_id: cinemaRoom.CINEMA_ROOM_ID,
                cinema_room_name: cinemaRoom.CINEMA_ROOM_NAME,
                seat_capacity: cinemaRoom.SEAT_CAPACITY
            }
            : null,
        seats: seatList
    };
};
