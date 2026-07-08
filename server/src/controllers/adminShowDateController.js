import mongoose from 'mongoose';
import { ShowDate } from '../models/showDateModel.js';
import { Showtime } from '../models/showTimeModel.js';
import { StatusCodes } from 'http-status-codes';

// GET /api/admin/show-dates
export const getShowDates = async (req, res) => {
    try {
        const filter = { isDeleted: false };
        const showDates = await ShowDate.find(filter).sort({ showDate: 1 }).lean();

        return res.status(StatusCodes.OK).json({
            success: true,
            data: showDates
        });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};

// POST /api/admin/show-dates
export const createShowDate = async (req, res) => {
    try {
        const { showDate, dateLabel } = req.body;

        if (!showDate) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'showDate field is required.'
            });
        }

        let targetDate;
        try {
            const dateStr = typeof showDate === 'string' ? showDate.substring(0, 10) : new Date(showDate).toISOString().substring(0, 10);
            targetDate = new Date(`${dateStr}T00:00:00.000Z`);
            if (isNaN(targetDate.getTime())) {
                throw new Error();
            }
        } catch (e) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid date format. Please use YYYY-MM-DD.'
            });
        }

        // Check if date already exists
        const existing = await ShowDate.findOne({ showDate: targetDate, isDeleted: false });
        if (existing) {
            return res.status(StatusCodes.CONFLICT).json({
                success: false,
                message: 'This show date already exists.'
            });
        }

        // Check if there's a soft-deleted record, reactivate it
        const softDeleted = await ShowDate.findOne({ showDate: targetDate, isDeleted: true });
        if (softDeleted) {
            softDeleted.isDeleted = false;
            softDeleted.deletedAt = null;
            if (dateLabel !== undefined) {
                softDeleted.dateLabel = dateLabel;
            }
            const updated = await softDeleted.save();
            return res.status(StatusCodes.CREATED).json({
                success: true,
                data: updated,
                message: 'Show date created/reactivated successfully.'
            });
        }

        const newShowDate = new ShowDate({ showDate: targetDate, dateLabel });
        const saved = await newShowDate.save();

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: saved,
            message: 'Show date created successfully.'
        });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};

// PUT /api/admin/show-dates/:id
export const updateShowDate = async (req, res) => {
    try {
        const { id } = req.params;
        const { showDate, dateLabel } = req.body;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid show date ID.'
            });
        }

        const existingRecord = await ShowDate.findOne({ _id: id, isDeleted: false });
        if (!existingRecord) {
            return res.status(StatusCodes.NOT_FOUND).json({
                success: false,
                message: 'Show date not found.'
            });
        }

        if (showDate !== undefined) {
            let targetDate;
            try {
                const dateStr = typeof showDate === 'string' ? showDate.substring(0, 10) : new Date(showDate).toISOString().substring(0, 10);
                targetDate = new Date(`${dateStr}T00:00:00.000Z`);
                if (isNaN(targetDate.getTime())) {
                    throw new Error();
                }
            } catch (e) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Invalid date format. Please use YYYY-MM-DD.'
                });
            }

            // Check unique constraint for the updated date value (excluding current record)
            const duplicate = await ShowDate.findOne({
                _id: { $ne: id },
                showDate: targetDate,
                isDeleted: false
            });
            if (duplicate) {
                return res.status(StatusCodes.CONFLICT).json({
                    success: false,
                    message: 'This show date value already exists on another record.'
                });
            }

            existingRecord.showDate = targetDate;
        }

        if (dateLabel !== undefined) {
            existingRecord.dateLabel = dateLabel;
        }

        const updated = await existingRecord.save();

        return res.status(StatusCodes.OK).json({
            success: true,
            data: updated,
            message: 'Show date updated successfully.'
        });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};

// DELETE /api/admin/show-dates/:id (soft delete)
export const deleteShowDate = async (req, res) => {
    try {
        const { id } = req.params;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid show date ID.'
            });
        }

        const existingRecord = await ShowDate.findOne({ _id: id, isDeleted: false });
        if (!existingRecord) {
            return res.status(StatusCodes.NOT_FOUND).json({
                success: false,
                message: 'Show date not found or already deleted.'
            });
        }

        const today = new Date();
        today.setUTCHours(0, 0, 0, 0); // UTC midnight of today

        // Check if the show date is today or in the future
        const isFutureOrToday = existingRecord.showDate >= today;

        if (isFutureOrToday) {
            // Check if any active showtimes are scheduled on this date
            const showtimeConflict = await Showtime.findOne({
                showDate: existingRecord.showDate,
                isDeleted: false
            });

            if (showtimeConflict) {
                return res.status(StatusCodes.CONFLICT).json({
                    success: false,
                    message: 'Cannot delete this show date because there are active showtimes scheduled on this date.'
                });
            }

            // Check dynamically if MovieDate model exists and is in use
            if (mongoose.models.MovieDate) {
                const movieDateConflict = await mongoose.models.MovieDate.findOne({
                    showDate: existingRecord.showDate,
                    isDeleted: false
                });

                if (movieDateConflict) {
                    return res.status(StatusCodes.CONFLICT).json({
                        success: false,
                        message: 'Cannot delete this show date because it is being used by MovieDate.'
                    });
                }
            }
        }

        existingRecord.isDeleted = true;
        existingRecord.deletedAt = new Date();
        const updated = await existingRecord.save();

        return res.status(StatusCodes.OK).json({
            success: true,
            data: updated,
            message: 'Show date deleted successfully.'
        });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};
