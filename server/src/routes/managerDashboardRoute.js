import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { managerDashboardController } from '../controllers/managerDashboardController.js';

const managerDashboardRouter = Router();

//GET /api/manager/dashboard - get dashboard info for manager
managerDashboardRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Manager"), managerDashboardController.getManagerDashboard);

export default managerDashboardRouter;