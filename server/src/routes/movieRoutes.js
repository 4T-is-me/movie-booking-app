import { Router } from 'express';
import {
    handleGetMovies,
    handleSearchMovies,
    handleGetMovieById,
    handleGetMovieShowtimes
} from '../controllers/movieController.js';

const router = Router();

// GET /api/movies - Lấy danh sách phim (lọc + phân trang)
router.get('/', handleGetMovies);

// GET /api/movies/search?q=... - Tìm kiếm phim theo tên VN/EN
router.get('/search', handleSearchMovies);

// GET /api/movies/:id - Lấy chi tiết một phim
router.get('/:id', handleGetMovieById);

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD - Lấy ngày + giờ chiếu của phim
router.get('/:id/showtimes', handleGetMovieShowtimes);

export default router;
