import express from 'express';
import { payrollConfigController } from '../controllers/payrollConfigController.js';
import { payrollConfigValidation } from '../validation/payrollConfigValidation.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const payrollConfigRouter = express.Router();

// GET /api/admin/payroll-config — Admin and Manager can read the current config
payrollConfigRouter.get(
  '/',
  authMiddleware.isAuthorized,
  authMiddleware.authorizeRoles('Admin', 'Manager'),
  payrollConfigController.getConfig
);

// PUT /api/admin/payroll-config — Only Admin can update
payrollConfigRouter.put(
  '/',
  authMiddleware.isAuthorized,
  authMiddleware.authorizeRoles('Admin'),
  payrollConfigValidation.updateConfig,
  payrollConfigController.updateConfig
);

export default payrollConfigRouter;
