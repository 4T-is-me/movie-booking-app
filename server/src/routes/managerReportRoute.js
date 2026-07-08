import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { managerReportController } from '../controllers/managerReportController.js';

const managerReportRouter = Router();

// GET /api/manager/reports
managerReportRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), managerReportController.getManagerReport);

export default managerReportRouter;