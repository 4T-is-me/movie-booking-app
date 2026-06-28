import mongoose from 'mongoose';
1
// POST /api/admin/showtimes
export const createShowtime = async (req, res) => {
    try {
        const { movieId, cinemaId, roomId, showDate, showTime, startTime, endTime } = req.body;

        if (!movieId || !cinemaId || !roomId || !showDate || !startTime || !endTime) {
            return res.status(400).json({
                success: false,
                message: 'Vui lòng điền đủ các trường bắt buộc: movieId, cinemaId, roomId, showDate, startTime, endTime.'
            });
        }

        const showtime = await showtimeAdminService.createShowtime(req.body);
        return res.status(201).json({
            success: true,
            data: showtime,
            message: 'Tạo suất chiếu thành công.'
        });
    } catch (error) {
        if (error.message === 'CONFLICT') {
            return res.status(409).json({
                success: false,
                message: 'Phòng chiếu đã có suất chiếu trùng thời gian.'
            });
        }
        return res.status(500).json({ success: false, message: 'Lỗi server.', error: error.message });
    }
};

// GET /api/admin/showtimes
export const getShowtimesAdmin = async (req, res) => {
    try {
        const { cinemaId, movieId, date, page = 1, limit = 20 } = req.query;

        const result = await showtimeAdminService.getShowtimesAdmin({
            cinemaId,
            movieId,
            date,
            page: Number(page),
            limit: Number(limit)
        });

        return res.status(200).json({ success: true, ...result });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server.', error: error.message });
    }
};

// GET /api/admin/showtimes/:id
export const getShowtimeDetail = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(400).json({ success: false, message: 'ID suất chiếu không hợp lệ.' });
        }

        const showtime = await showtimeAdminService.getShowtimeDetail(id);

        if (!showtime) {
            return res.status(404).json({ success: false, message: 'Không tìm thấy suất chiếu.' });
        }

        return res.status(200).json({ success: true, data: showtime });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server.', error: error.message });
    }
};

// DELETE /api/admin/showtimes/:id  (soft delete)
export const softDeleteShowtime = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(400).json({ success: false, message: 'ID suất chiếu không hợp lệ.' });
        }

        const deleted = await showtimeAdminService.softDeleteShowtime(id);

        if (!deleted) {
            return res.status(404).json({ success: false, message: 'Không tìm thấy suất chiếu.' });
        }

        return res.status(200).json({ success: true, message: 'Đã xóa suất chiếu thành công.' });
    } catch (error) {
        if (error.message === 'ALREADY_DELETED') {
            return res.status(409).json({ success: false, message: 'Suất chiếu đã bị xóa trước đó.' });
        }
        return res.status(500).json({ success: false, message: 'Lỗi server.', error: error.message });
    }
};
