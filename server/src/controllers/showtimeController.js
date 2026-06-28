import mongoose from 'mongoose';
import { showtimeService } from '../services/showtimeService.js';
import { StatusCodes} from 'http-status-codes';
// GET /api/showtimes/:id/seats
const handleGetShowtimeSeats = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(StatusCodes.NOT_FOUND).json({ message: 'Không tìm thấy suất chiếu.' });
        }

        const result = await showtimeService.getShowtimeSeats(id);

        if (!result) {
            return res.status(StatusCodes.NOT_FOUND).json({ message: 'Không tìm thấy suất chiếu.' });
        }

        return res.status(StatusCodes.OK).json({ data: result });
    } catch (error) {
        console.error('handleGetShowtimeSeats error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
}
///api/showtimes/movie/id
const handleGetMovieShowtimes = async (req, res) => {
    try {
        const { movieId } = req.params;
        const result = await showtimeService.getMovieShowtimes(movieId);
        return res.status(StatusCodes.OK).json({
            data: result
        });

    } catch (e) {
        console.error(e);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
            message: e.message
        });
    }
}
export const showtimeController = {
    handleGetShowtimeSeats,
    handleGetMovieShowtimes
}

