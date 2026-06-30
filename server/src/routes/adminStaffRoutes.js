import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import {
    getStaffList,
    createStaff,
    updateStaff,
    deleteStaff
} from '../controllers/adminStaffController.js';

const router = Router();

// All routes require authentication and Admin authorization
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin'));

// GET /api/admin/staff - Get staff list with pagination & filters
router.get('/', getStaffList);

// POST /api/admin/staff - Create a new staff member
router.post('/', createStaff);

// PUT /api/admin/staff/:id - Update staff details
router.put('/:id', updateStaff);

// DELETE /api/admin/staff/:id - Lock staff account (soft delete)
router.delete('/:id', deleteStaff);

export default router;
