import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { bookingController } from '../controllers/bookingController.js';
import { showtimeController } from '../controllers/showtimeController.js';
const router = Router();

// GET /api/showtimes/:id/seats - Lấy sơ đồ ghế của suất chiếu
router.get('/:id/seats', showtimeController.handleGetShowtimeSeats);
router.get('/movie/:movieId', showtimeController.handleGetMovieShowtimes);
router.post('/:id/hold', authMiddleware.isAuthorized, bookingController.handleHoldSeats);
router.post('/:id/release', authMiddleware.isAuthorized, bookingController.handleReleaseBooking);
export default router;
