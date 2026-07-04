import * as adminMemberService from '../services/adminMemberService.js';

/**
 * get list member for admin
 */
export const getMemberList = async (req, res) => {
    try {
        const { page = 1, limit = 10, status, search } = req.query;
        const result = await adminMemberService.getMemberList({
            page: Number(page),
            limit: Number(limit),
            status,
            search
        });
        
        return res.status(200).json({
            success: true,
            data: result,
            message: 'Member list retrieved successfully.'
        });
    } catch (error) {
        return res.status(500).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};

/**
 * adjust score member
 */
export const adjustMemberScore = async (req, res) => {
    try {
        const { id } = req.params;
        const { adjustment, reason } = req.body;

        if (adjustment === undefined || typeof adjustment !== 'number' || adjustment === 0) {
            return res.status(400).json({
                success: false,
                message: 'Adjustment score must be a non-zero number.'
            });
        }

        if (!reason || typeof reason !== 'string' || reason.trim() === '') {
            return res.status(400).json({
                success: false,
                message: 'Adjustment reason cannot be empty.'
            });
        }

        const result = await adminMemberService.adjustMemberScore(id, {
            adjustment,
            reason: reason.trim()
        });

        return res.status(200).json({
            success: true,
            data: result,
            message: 'Loyalty points adjusted successfully.'
        });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({
                success: false,
                message: error.message
            });
        }
        if (error.name === 'CastError') {
            return res.status(400).json({
                success: false,
                message: 'Invalid member ID.'
            });
        }
        return res.status(500).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};

/**
 * update status member account
 */
export const updateMemberStatus = async (req, res) => {
    try {
        const { id } = req.params;
        const { status } = req.body;

        if (status === undefined || typeof status !== 'number') {
            return res.status(400).json({
                success: false,
                message: 'Status must be a number.'
            });
        }

        const result = await adminMemberService.updateMemberStatus(id, status);
        if (!result) {
            return res.status(404).json({
                success: false,
                message: 'Member not found.'
            });
        }

        return res.status(200).json({
            success: true,
            data: result,
            message: 'Member account status updated successfully.'
        });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({
                success: false,
                message: error.message
            });
        }
        if (error.name === 'CastError') {
            return res.status(400).json({
                success: false,
                message: 'Invalid member ID.'
            });
        }
        return res.status(500).json({
            success: false,
            message: 'Internal server error.',
            error: error.message
        });
    }
};
