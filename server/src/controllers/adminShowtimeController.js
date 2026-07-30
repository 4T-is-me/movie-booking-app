import mongoose from 'mongoose';
import * as showtimeAdminService from '../services/adminShowtimeService.js';
import { constants } from '../utils/constants.js';
import { Manager } from '../models/managerModel.js';

// POST /api/admin/showtimes
export const createShowtime = async (req, res) => {
    try {
        const { movieId, cinemaId, roomId, showDate, showTime, ticketPrice } = req.body;

        if (!movieId || !cinemaId || !roomId || !showDate || !showTime || !ticketPrice) {
            return res.status(400).json({
                success: false,
                message: 'Please fill in all required fields: movieId, cinemaId, roomId, showDate, showTime, ticketPrice.'
            });
        }

        const showtime = await showtimeAdminService.createShowtime(req.body);
        return res.status(201).json({
            success: true,
            data: showtime,
            message: 'Showtime created successfully.'
        });
    } catch (error) {
        if (error.message === 'CONFLICT') {
            return res.status(409).json({
                success: false,
                message: 'This cinema room already has a showtime at the same time.'
            });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

// POST /api/admin/showtimes/bulk
export const createBulkShowtimes = async (req, res) => {
    try {
        const { showtimes } = req.body;

        if (!showtimes || !Array.isArray(showtimes) || showtimes.length === 0) {
            return res.status(400).json({
                success: false,
                message: 'Please provide an array of showtimes.'
            });
        }

        let defaultCinemaId = null;
        if (req.user && req.user.role === constants.ROLES.MANAGER) {
            const manager = await Manager.findOne({ accountId: req.user.accountId });
            if (manager) {
                defaultCinemaId = manager.cinemaId;
            }
        }

        // Thay thế cinemaId ảo từ Frontend (ví dụ "c1") thành cinemaId thật của Manager
        const formattedShowtimes = showtimes.map(st => {
            if (defaultCinemaId && (!st.cinemaId || st.cinemaId === 'c1')) {
                return { ...st, cinemaId: defaultCinemaId };
            }
            return st;
        });

        const createdShowtimes = await showtimeAdminService.createBulkShowtimes(formattedShowtimes);
        return res.status(201).json({
            success: true,
            data: createdShowtimes,
            message: `Successfully bulk inserted ${createdShowtimes.length} showtimes.`
        });
    } catch (error) {
        console.error("Bulk insert error:", error);
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

// GET /api/admin/showtimes
export const getShowtimesAdmin = async (req, res) => {
    try {
        const { movieId, cinemaId, showDate, startDate, endDate, page = 1, limit = 20 } = req.query;

        // If manager, auto-restrict to their cinema
        let filterCinemaId = cinemaId;
        if (req.user && req.user.role === constants.ROLES.MANAGER) {
            const manager = await Manager.findOne({ accountId: req.user.accountId });
            if (manager) filterCinemaId = manager.cinemaId;
        }

        const result = await showtimeAdminService.getShowtimesAdmin({
            movieId,
            cinemaId: filterCinemaId,
            showDate,
            startDate,
            endDate,
            page: Number(page),
            limit: Number(limit)
        });

        return res.status(200).json({ success: true, ...result });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

// GET /api/admin/showtimes/:id
export const getShowtimeDetail = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(400).json({ success: false, message: 'Invalid showtime ID.' });
        }

        const showtime = await showtimeAdminService.getShowtimeDetail(id);

        if (!showtime) {
            return res.status(404).json({ success: false, message: 'Showtime not found.' });
        }

        return res.status(200).json({ success: true, data: showtime });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

// DELETE /api/admin/showtimes/:id  (soft delete)
export const softDeleteShowtime = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(400).json({ success: false, message: 'Invalid showtime ID.' });
        }

        const deleted = await showtimeAdminService.softDeleteShowtime(id);

        if (!deleted) {
            return res.status(404).json({ success: false, message: 'Showtime not found.' });
        }

        return res.status(200).json({ success: true, message: 'Showtime deleted successfully.' });
    } catch (error) {
        if (error.message === 'ALREADY_DELETED') {
            return res.status(409).json({ success: false, message: 'Showtime was already deleted.' });
        }
        if (error.message === 'HAS_BOOKINGS') {
            return res.status(409).json({ success: false, message: 'Cannot delete showtime that has booked or held seats.' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};
