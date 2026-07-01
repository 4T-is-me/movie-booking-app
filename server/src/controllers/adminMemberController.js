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
            message: 'Lấy danh sách thành viên thành công.'
        });
    } catch (error) {
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
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
                message: 'Số điểm điều chỉnh (adjustment) phải là số khác 0.'
            });
        }

        if (!reason || typeof reason !== 'string' || reason.trim() === '') {
            return res.status(400).json({
                success: false,
                message: 'Lý do điều chỉnh (reason) không được để trống.'
            });
        }

        const result = await adminMemberService.adjustMemberScore(id, {
            adjustment,
            reason: reason.trim()
        });

        return res.status(200).json({
            success: true,
            data: result,
            message: 'Điều chỉnh điểm tích lũy thành công.'
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
                message: 'ID thành viên không hợp lệ.'
            });
        }
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
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
                message: 'Trạng thái (status) phải là số.'
            });
        }

        const result = await adminMemberService.updateMemberStatus(id, status);
        if (!result) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy thành viên.'
            });
        }

        return res.status(200).json({
            success: true,
            data: result,
            message: 'Cập nhật trạng thái tài khoản thành viên thành công.'
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
                message: 'ID thành viên không hợp lệ.'
            });
        }
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            error: error.message
        });
    }
};
