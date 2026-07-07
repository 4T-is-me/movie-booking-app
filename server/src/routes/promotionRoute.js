import express from 'express';
import { promotionController } from '../controllers/promotionController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = express.Router();

// POST /api/promotions/apply
router.post('/apply', authMiddleware.isAuthorized, promotionController.applyPromotion);

// POST /api/promotions/validate
router.post('/validate', authMiddleware.isAuthorized, promotionController.validatePromotion);

// GET /api/promotions
router.get('/', authMiddleware.isAuthorized, promotionController.getPromotions);

export default router;
