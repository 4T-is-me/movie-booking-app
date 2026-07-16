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
        
        return res.status(200).json({ success: true, data: result, message: 'Staff list retrieved successfully.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const createStaff = async (req, res) => {
    try {
        console.log("CREATE STAFF BODY:", req.body);
        const { username, email, password, roleId, role, ...staffData } = req.body;
        const accountData = { username, email, password, roleId, role };
        const staff = await adminStaffService.createStaff(accountData, staffData);

        return res.status(201).json({ success: true, data: staff, message: 'Staff account created successfully.' });
    } catch (error) {
        if (error.code === 11000) {
            return res.status(400).json({ success: false, message: 'Data already exists (Username, Email, Identity Card, or this Cinema already has a Manager).' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const updateStaff = async (req, res) => {
    try {
        const { id } = req.params;
        const staff = await adminStaffService.updateStaff(id, req.body);
        
        if (!staff) return res.status(404).json({ success: false, message: 'Staff not found.' });
        
        return res.status(200).json({ success: true, data: staff, message: 'Staff details updated successfully.' });
    } catch (error) {
        if (error.message === 'EmailAlreadyExists') {
            return res.status(400).json({ success: false, message: 'Email is already in use by another account.' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const deleteStaff = async (req, res) => {
    try {
        const { id } = req.params;
        await adminStaffService.softDeleteStaff(id);
        
        return res.status(200).json({ success: true, message: 'Staff account disabled successfully.' });
    } catch (error) {
        if (error.message === 'StaffNotFound') {
            return res.status(404).json({ success: false, message: 'Staff not found.' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const updateStaffStatus = async (req, res) => {
    try {
        const { id } = req.params;
        const { status } = req.body;

        const staff = await adminStaffService.updateStaffStatus(id, status);
        
        if (!staff) {
            return res.status(404).json({ success: false, message: 'Staff not found.' });
        }
        
        return res.status(200).json({ 
            success: true, 
            data: staff, 
            message: 'Staff account status updated successfully.' 
        });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const updateStaffRole = async (req, res) => {
    try {
        const { id } = req.params;
        const { role, roleId } = req.body;

        const staff = await adminStaffService.updateStaffRole(id, { role, roleId });
        
        if (!staff) {
            return res.status(404).json({ success: false, message: 'Staff not found.' });
        }
        
        return res.status(200).json({ 
            success: true, 
            data: staff, 
            message: 'New role assigned to staff successfully.' 
        });
    } catch (error) {
        if (error.message === 'RoleNotFound') {
            return res.status(400).json({ success: false, message: 'Role does not exist in the system.' });
        }
        if (error.message === 'InvalidRole') {
            return res.status(400).json({ success: false, message: 'Invalid role. Only Staff, Manager, or Admin are accepted.' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};
