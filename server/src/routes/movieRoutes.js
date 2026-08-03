import { Router } from 'express';
import { handleGetMovies, handleSearchMovies, handleGetMovieById, handleGetMovieShowtimes } from '../controllers/movieController.js';
import { handleCreateReview, handleGetMovieReviews } from '../controllers/reviewController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = Router();

// GET /api/movies - Get list of movies (filtering + pagination)
router.get('/', handleGetMovies);

// GET /api/movies/search?q=... - Search movies by name (Vietnamese/English)
router.get('/search', handleSearchMovies);

// GET /api/movies/:id - Get movie details
router.get('/:id', handleGetMovieById);

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD - Get showtimes and dates of a movie
router.get('/:id/showtimes', handleGetMovieShowtimes);

// GET /api/movies/:id/reviews - Get reviews list for a movie (public)
router.get('/:id/reviews', handleGetMovieReviews);

// POST /api/movies/:id/reviews - Create a review for a movie (requires authentication)
router.post('/:id/reviews', authMiddleware.isAuthorized, handleCreateReview);

export default router;
