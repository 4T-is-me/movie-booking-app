import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { createShowtime, createBulkShowtimes, getShowtimesAdmin, getShowtimeDetail, softDeleteShowtime } from '../controllers/adminShowtimeController.js';

const router = Router();

// All routes in this router are protected and require the user to be authenticated and authorized as either an Admin or Manager.
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// POST   /api/admin/showtimes/bulk   — bulk create showtimes (for auto-scheduling)
router.post('/bulk', createBulkShowtimes);

// POST   /api/admin/showtimes        — create a new showtime
router.post('/', createShowtime);

// GET    /api/admin/showtimes        — get list of showtimes with optional filters (cinemaId, movieId, date) and pagination
router.get('/', getShowtimesAdmin);

// GET    /api/admin/showtimes/:id    — get detail of a specific showtime
router.get('/:id', getShowtimeDetail);

// DELETE /api/admin/showtimes/:id    — soft delete a showtime
router.delete('/:id', softDeleteShowtime);

export default router;
