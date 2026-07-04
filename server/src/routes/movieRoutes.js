import { Router } from 'express';
import {
    handleGetMovies,
    handleSearchMovies,
    handleGetMovieById,
    handleGetMovieShowtimes
} from '../controllers/movieController.js';

const router = Router();

// GET /api/movies - Get list of movies (filtering + pagination)
router.get('/', handleGetMovies);

// GET /api/movies/search?q=... - Search movies by name (Vietnamese/English)
router.get('/search', handleSearchMovies);

// GET /api/movies/:id - Get movie details
router.get('/:id', handleGetMovieById);

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD - Get showtimes and dates of a movie
router.get('/:id/showtimes', handleGetMovieShowtimes);

export default router;
