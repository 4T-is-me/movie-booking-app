import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import {
    getShowDates,
    createShowDate,
    updateShowDate,
    deleteShowDate
} from '../controllers/adminShowDateController.js';

const router = Router();

// Protect all endpoints within this router to be authorized for Admin and Manager roles.
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// GET /api/admin/show-dates — get list of show dates with optional pagination
router.get('/', getShowDates);

// POST /api/admin/show-dates — create a new show date
router.post('/', createShowDate);

// PUT /api/admin/show-dates/:id — update a show date by ID
router.put('/:id', updateShowDate);

// DELETE /api/admin/show-dates/:id — soft delete a show date by ID
router.delete('/:id', deleteShowDate);

export default router;
