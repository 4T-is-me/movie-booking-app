import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { bookingController } from '../controllers/bookingController.js';
import { showtimeController } from '../controllers/showtimeController.js';
const router = Router();

// GET /api/showtimes/:id/seats - Get the seat map for the screening.
router.get('/:id/seats', showtimeController.handleGetShowtimeSeats);
// GET /api/showtimes/movie/:movieId - Get the movie's screening date and time.
router.get('/movie/:movieId', showtimeController.handleGetMovieShowtimes);
// POST /api/showtimes/:id/hold - Hold a seat for a booking.
router.post('/:id/hold', authMiddleware.isAuthorized, bookingController.handleHoldSeats);
// POST /api/showtimes/:id/release - Release a seat for a booking.
router.post('/:id/release', authMiddleware.isAuthorized, bookingController.handleReleaseBooking);
export default router;
