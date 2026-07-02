import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { getOverviewStats, getMovieRevenueStats, getDailyRevenue, getMonthlyRevenue, getRevenueByMovie } from '../controllers/adminStatsController.js';

const router = Router();

// Protect all routes under this router: Only accessible by authenticated Admins or Managers
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin', 'Manager'));

// GET /api/admin/stats/overview
router.get('/overview', getOverviewStats);

// GET /api/admin/stats/movie-revenue
router.get('/movie-revenue', getMovieRevenueStats);

// GET /api/admin/stats/revenue/daily
router.get('/revenue/daily', getDailyRevenue);

// GET /api/admin/stats/revenue/monthly
router.get('/revenue/monthly', getMonthlyRevenue);

// GET /api/admin/stats/revenue/by-movie
router.get('/revenue/by-movie', getRevenueByMovie);

export default router;
