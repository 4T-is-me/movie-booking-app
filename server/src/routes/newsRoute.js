import express from 'express';
import { newsController } from '../controllers/newsController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = express.Router();

// Publicly viewable news
router.get('/', newsController.getNews);
router.get('/:id', newsController.getNewsById);

// Restrict all comment/social interactions to authenticated users
router.use(authMiddleware.isAuthorized);

router.get('/:id/comments', newsController.getComments);
router.post('/:id/comments', newsController.createComment);
router.post('/comments/:commentId/react', newsController.toggleReaction);

export const newsRoute = router;
