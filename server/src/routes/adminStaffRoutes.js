import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { adminStaffValidation } from '../validation/adminStaffValidation.js';
import {
    getStaffList,
    createStaff,
    updateStaff,
    deleteStaff,
    updateStaffStatus,
    updateStaffRole
} from '../controllers/adminStaffController.js';

const router = Router();

// All routes require authentication and Admin authorization
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin'));

// GET /api/admin/staff - Get staff list with pagination & filters
router.get('/', getStaffList);

// POST /api/admin/staff - Create a new staff member
router.post('/', adminStaffValidation.createStaff, createStaff);

// PUT /api/admin/staff/:id - Update staff details
router.put('/:id', adminStaffValidation.updateStaff, updateStaff);

// PATCH /api/admin/staff/:id/status - Update staff account status
router.patch('/:id/status', adminStaffValidation.updateStaffStatus, updateStaffStatus);

// PATCH /api/admin/staff/:id/role - Assign new role
router.patch('/:id/role', adminStaffValidation.updateStaffRole, updateStaffRole);

// DELETE /api/admin/staff/:id - Lock staff account (soft delete)
router.delete('/:id', deleteStaff);

export default router;
