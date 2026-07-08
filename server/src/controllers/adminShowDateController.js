import mongoose from 'mongoose';
import { ShowDate } from '../models/showDateModel.js';
import { StatusCodes } from 'http-status-codes';

// GET /api/admin/show-dates
export const getShowDates = async (req, res) => {
    try {
        const { page = 1, limit = 20, all = false } = req.query;
        
        const filter = { isDeleted: false };
        
        if (all === 'true' || all === true) {
            const showDates = await ShowDate.find(filter).sort({ showDate: 1 }).lean();
            return res.status(StatusCodes.OK).json({
                success: true,
                data: showDates
            });
        }

        const skip = (Number(page) - 1) * Number(limit);
        const total = await ShowDate.countDocuments(filter);
        const showDates = await ShowDate.find(filter)
            .sort({ showDate: 1 })
            .skip(skip)
            .limit(Number(limit))
            .lean();

        return res.status(StatusCodes.OK).json({
            success: true,
            page: Number(page),
            limit: Number(limit),
            total,
            totalPages: Math.ceil(total / Number(limit)),
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
        const { showDate } = req.body;

        if (!showDate) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'showDate field is required.'
            });
        }

        const targetDate = new Date(showDate);
        if (isNaN(targetDate.getTime())) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid date format.'
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
            const updated = await softDeleted.save();
            return res.status(StatusCodes.CREATED).json({
                success: true,
                data: updated,
                message: 'Show date created/reactivated successfully.'
            });
        }

        const newShowDate = new ShowDate({ showDate: targetDate });
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
        const { showDate } = req.body;

        if (!mongoose.isValidObjectId(id)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid show date ID.'
            });
        }

        if (!showDate) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'showDate field is required.'
            });
        }

        const targetDate = new Date(showDate);
        if (isNaN(targetDate.getTime())) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid date format.'
            });
        }

        const existingRecord = await ShowDate.findOne({ _id: id, isDeleted: false });
        if (!existingRecord) {
            return res.status(StatusCodes.NOT_FOUND).json({
                success: false,
                message: 'Show date not found.'
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
