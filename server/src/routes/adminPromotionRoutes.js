import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import * as adminPromotionController from '../controllers/adminPromotionController.js';

const router = Router();

// Protect routes
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// GET /api/admin/promotions - Get all promotions
router.get('/', adminPromotionController.getPromotions);

// POST /api/admin/promotions - Create a new promotion
router.post('/', adminPromotionController.createPromotion);

// PUT /api/admin/promotions/:id - Update an existing promotion
router.put('/:id', adminPromotionController.updatePromotion);

// DELETE /api/admin/promotions/:id - Soft delete a promotion
router.delete('/:id', adminPromotionController.deletePromotion);

// POST /api/admin/promotions/broadcast-push - Send a custom push broadcast to all users (topic promotions)
router.post('/broadcast-push', adminPromotionController.broadcastPushNotification);

export default router;
