import { Router } from 'express';
import { handleToggleLike, handleCreateReply } from '../controllers/reviewController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = Router();

// POST /api/reviews/:reviewId/like - Toggle like on a review (requires auth)
router.post('/:reviewId/like', authMiddleware.isAuthorized, handleToggleLike);

// POST /api/reviews/:reviewId/replies - Add a reply to a review (requires auth)
router.post('/:reviewId/replies', authMiddleware.isAuthorized, handleCreateReply);

export default router;
