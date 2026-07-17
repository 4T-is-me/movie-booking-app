import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { adminCinemaController } from '../controllers/adminCinemaController.js';
import { uploadMiddleware } from '../middlewares/uploadCloud.js';

const adminCinemaRouter = Router();

// POST /api/admin/cinemas - create a new cinema
adminCinemaRouter.post(
    '/',
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    uploadMiddleware.uploadIfMultipart,
    uploadMiddleware.uploadToCloudinary('cinemas_images', 'image'),
    uploadMiddleware.normalizeMultipartBody,
    adminCinemaController.createCinema
);
// GET /api/admin/cinemas - get all cinemas
adminCinemaRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Admin"), adminCinemaController.getAllCinemas);

// GET /api/admin/cinemas/:id - get cinema details
adminCinemaRouter.get('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Admin"), adminCinemaController.getCinemaDetails);

// PUT /api/admin/cinemas/:id - update cinema
adminCinemaRouter.put(
    '/:id',
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    uploadMiddleware.uploadIfMultipart,
    uploadMiddleware.uploadToCloudinary('cinemas_images', 'image'),
    uploadMiddleware.normalizeMultipartBody,
    adminCinemaController.updateCinema
);

// PATCH /api/admin/cinemas/:id/status - soft delete / reactivate cinema
adminCinemaRouter.patch('/:id/status', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Admin"), adminCinemaController.toggleCinemaStatus);

export const adminCinemaRoute = {
    adminCinemaRouter
};
