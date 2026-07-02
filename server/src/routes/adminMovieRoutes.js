import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { adminMovieController } from '../controllers/adminMovieController.js';
import { uploadMiddleware } from '../middlewares/uploadCloud.js';
import { AdminMovieValidation } from '../validation/adminMovieValidation.js';

const router = Router();

// All routes require authentication and admin/manager authorization
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// POST /api/admin/movies - Create a new movie
router.post('/', uploadMiddleware.upload.single('poster'), uploadMiddleware.uploadToCloudinary('movies_poster', 'poster'), AdminMovieValidation.createMovie, adminMovieController.createMovie);

// PUT /api/admin/movies/:id - Update an existing movie
router.put('/:id', uploadMiddleware.upload.single('poster'), uploadMiddleware.uploadToCloudinary('movies_poster', 'poster'), AdminMovieValidation.updateMovie, adminMovieController.updateMovie);

// DELETE /api/admin/movies/:id - Delete a movie
router.delete('/:id', adminMovieController.deleteMovie);

// POST /api/admin/movies/:id/cinemas - Assign a movie to cinemas
router.post('/:id/cinemas', adminMovieController.assignMovieToCinemas);

// GET /api/admin/movies - Get all movies (with optional filters)
router.get('/', adminMovieController.getMoviesAdmin);

export default router;