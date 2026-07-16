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

// Hàm dành riêng cho việc lưu lịch auto-scheduling (Lưu hàng loạt)
export const createBulkShowtimes = async (showtimesData) => {
    const createdShowtimes = [];
    const allScheduleSeats = [];
    const seatsCache = {};

    for (const data of showtimesData) {
        const { movie_id, cinema_id, room_id, show_date, start_time, end_time, status, ticketPrice } = data;
        
        // Theo chuẩn frontend JSON có thể truyền snake_case, ta parse lại
        const movieId = data.movieId || movie_id;
        const cinemaId = data.cinemaId || cinema_id;
        const roomId = data.roomId || room_id;
        const showDate = data.showDate || show_date;
        const startTimeRaw = data.startTime || start_time;
        const endTimeRaw = data.endTime || end_time;

        // Bổ sung múi giờ +07:00 nếu chưa có để NodeJS trên server (UTC) parse thời gian chính xác
        let startTime = new Date(startTimeRaw);
        if (typeof startTimeRaw === 'string' && !startTimeRaw.includes('Z') && !startTimeRaw.includes('+')) {
            startTime = new Date(`${startTimeRaw}+07:00`);
        }

        let endTime = new Date(endTimeRaw);
        if (typeof endTimeRaw === 'string' && !endTimeRaw.includes('Z') && !endTimeRaw.includes('+')) {
            endTime = new Date(`${endTimeRaw}+07:00`);
        }

        // Tự động tạo showTime dạng "HH:mm" từ startTimeRaw
        let showTime = "";
        if (typeof startTimeRaw === 'string') {
            const timePart = startTimeRaw.split('T')[1];
            if (timePart) {
                showTime = timePart.substring(0, 5); // Lấy "08:30"
            }
        }

        // Kiểm tra xem suất chiếu đã tồn tại chưa để tránh tạo trùng lặp
        const existingShowtime = await Showtime.findOne({
            roomId: roomId,
            startTime: startTime,
            isDeleted: false
        });

        if (existingShowtime) {
            console.log(`Showtime already exists for room ${roomId} at ${startTime}. Skipping.`);
            continue; // Bỏ qua suất chiếu bị trùng
        }

        const showtime = new Showtime({
            movieId,
            cinemaId,
            roomId,
            showDate,
            showTime,
            startTime,
            endTime,
            ticketPrice: ticketPrice || 80000, // Giá vé mặc định
            status: status || 'draft', // Auto schedule thường lưu dưới dạng draft
            isDeleted: false
        });
        
        const savedShowtime = await showtime.save();
        createdShowtimes.push(savedShowtime);

        // Cache ghế để tối ưu tốc độ khi có nhiều suất trong cùng 1 phòng
        if (!seatsCache[roomId]) {
            seatsCache[roomId] = await Seat.find({ roomId });
        }
        
        const seats = seatsCache[roomId];
        const scheduleSeats = seats.map(seat => ({
            showtimeId: savedShowtime._id,
            seatId: seat._id,
            status: 1 // 1 = empty
        }));
        
        allScheduleSeats.push(...scheduleSeats);
    }
    
    // Batch insert schedule seats để không quá tải DB
    if (allScheduleSeats.length > 0) {
        // Chia nhỏ chunk 1000 records nếu mảng quá lớn
        const chunkSize = 1000;
        for (let i = 0; i < allScheduleSeats.length; i += chunkSize) {
            const chunk = allScheduleSeats.slice(i, i + chunkSize);
            await ScheduleSeat.insertMany(chunk);
        }
    }
    
    return createdShowtimes;
};


export const getShowtimesAdmin = async ({ movieId, cinemaId, showDate, startDate, endDate, page = 1, limit = 20 }) => {
    const filter = { isDeleted: false };
    if (movieId) filter.movieId = movieId;
    if (cinemaId) filter.cinemaId = cinemaId;
    
    // Support specific date or date range
    if (showDate) {
        const date = new Date(showDate);
        filter.showDate = {
            $gte: new Date(date.setHours(0, 0, 0, 0)),
            $lt: new Date(date.setHours(23, 59, 59, 999))
        };
    } else if (startDate && endDate) {
        const start = new Date(startDate);
        const end = new Date(endDate);
        filter.showDate = {
            $gte: new Date(start.setHours(0, 0, 0, 0)),
            $lt: new Date(end.setHours(23, 59, 59, 999))
        };
    }

    const skip = (page - 1) * limit;

    const [showtimes, total] = await Promise.all([
        Showtime.find(filter)
            .populate('movieId', 'movieName duration')
            .populate('cinemaId', 'cinemaName')
            .populate('roomId', 'roomName')
            .sort({ showDate: -1, startTime: -1 }) // Sort by newest date, then newest time
            .skip(skip)
            .limit(limit)
            .lean(),
        Showtime.countDocuments(filter)
    ]);

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
