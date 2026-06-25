import { Router } from 'express';
import { handleGetShowtimeSeats } from '../controllers/showtimeController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { bookingController } from '../controllers/bookingController.js';
const router = Router();

// GET /api/showtimes/:id/seats - Lấy sơ đồ ghế của suất chiếu
router.get('/:id/seats', handleGetShowtimeSeats);
router.post('/:id/hold', authMiddleware.isAuthorized, bookingController.handleHoldSeats);
router.post('/:id/release', authMiddleware.isAuthorized, bookingController.handleReleaseBooking);
export default router;
