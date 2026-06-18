import { Router } from 'express';
import { handleGetShowtimeSeats } from '../controllers/showtimeController.js';

const router = Router();

// GET /api/showtimes/:id/seats - Lấy sơ đồ ghế của suất chiếu
router.get('/:id/seats', handleGetShowtimeSeats);

export default router;
