import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import {
    createMovie,
    updateMovie,
    deleteMovie,
    assignMovieToCinemas,
    getMoviesAdmin
} from '../controllers/adminMovieController.js';

const router = Router();

// All routes require authentication and admin/manager authorization
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// POST /api/admin/movies - Create a new movie
router.post('/', createMovie);

// PUT /api/admin/movies/:id - Update an existing movie
router.put('/:id', updateMovie);

// DELETE /api/admin/movies/:id - Delete a movie
router.delete('/:id', deleteMovie);

// POST /api/admin/movies/:id/cinemas - Assign a movie to cinemas
router.post('/:id/cinemas', assignMovieToCinemas);

// GET /api/admin/movies - Get all movies (with optional filters)
router.get('/', getMoviesAdmin)
export default router;
