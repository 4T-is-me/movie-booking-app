import express from 'express';
import rateLimit from 'express-rate-limit';
import { promotionController } from '../controllers/promotionController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = express.Router();

// Giới hạn số lần thử nhập coupon: 10 lần trong 15 phút
const couponRateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    message: {
        success: false,
        message: "Bạn đã thử nhập mã quá nhiều lần. Vui lòng thử lại sau 15 phút!"
    }
});

// POST /api/promotions/apply
router.post('/apply', authMiddleware.isAuthorized, couponRateLimiter, promotionController.applyPromotion);

// POST /api/promotions/validate
router.post('/validate', authMiddleware.isAuthorized, couponRateLimiter, promotionController.validatePromotion);

// GET /api/promotions
router.get('/', authMiddleware.isAuthorized, promotionController.getPromotions);

export default router;
