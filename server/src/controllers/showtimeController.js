import { getShowtimeSeats } from '../services/showtimeService.js';

// GET /api/showtimes/:id/seats
export const handleGetShowtimeSeats = async (req, res) => {
    try {
        const { id } = req.params;
        const result = await getShowtimeSeats(id);
        if (!result) {
            return res.status(404).json({ message: 'Không tìm thấy suất chiếu.' });
        }
        return res.status(200).json({ data: result });
    } catch (error) {
        console.error('handleGetShowtimeSeats error:', error);
        return res.status(500).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};
