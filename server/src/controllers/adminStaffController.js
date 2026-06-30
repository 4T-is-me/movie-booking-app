import * as adminStaffService from '../services/adminStaffService.js';

export const getStaffList = async (req, res) => {
    try {
        const { page = 1, limit = 10, status, role, search } = req.query;
        const result = await adminStaffService.getStaffList({
            page: Number(page),
            limit: Number(limit),
            status,
            role,
            search
        });
        
        return res.status(200).json({ success: true, data: result, message: 'Lấy danh sách nhân viên thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const createStaff = async (req, res) => {
    try {
        const { username, email, password, roleId, role, ...staffData } = req.body;
        const accountData = { username, email, password, roleId, role };
        const staff = await adminStaffService.createStaff(accountData, staffData);

        return res.status(201).json({ success: true, data: staff, message: 'Tạo tài khoản nhân viên (Staff) thành công.' });
    } catch (error) {
        if (error.code === 11000) {
            return res.status(400).json({ success: false, message: 'Dữ liệu đã tồn tại (Username/Email hoặc CMND bị trùng).' });
        }
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const updateStaff = async (req, res) => {
    try {
        const { id } = req.params;
        const staff = await adminStaffService.updateStaff(id, req.body);
        
        if (!staff) return res.status(404).json({ success: false, message: 'Không tìm thấy nhân viên (Staff).' });
        
        return res.status(200).json({ success: true, data: staff, message: 'Cập nhật thông tin thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const deleteStaff = async (req, res) => {
    try {
        const { id } = req.params;
        await adminStaffService.softDeleteStaff(id);
        
        return res.status(200).json({ success: true, message: 'Đã khóa tài khoản nhân viên thành công (Xóa mềm).' });
    } catch (error) {
        if (error.message === 'StaffNotFound') {
            return res.status(404).json({ success: false, message: 'Không tìm thấy nhân viên.' });
        }
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};
