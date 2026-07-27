import express from 'express';
import { payrollController } from '../controllers/payrollController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const payrollRouter = express.Router();
// POST /api/payrolls/generate
payrollRouter.post('/generate', authMiddleware.isAuthorized, payrollController.generatePayroll);
// GET /api/payrolls
payrollRouter.get('/', authMiddleware.isAuthorized, payrollController.getAllPayrolls);
// GET /api/payrolls/manager
payrollRouter.get('/manager', authMiddleware.isAuthorized, payrollController.getManagerPayrolls);
// GET /api/payrolls/staff
payrollRouter.get('/staff', authMiddleware.isAuthorized, payrollController.getMyPayroll);
// GET /api/payrolls/:id
payrollRouter.get('/:id', authMiddleware.isAuthorized, payrollController.getPayrollById);
// PATCH /api/payrolls/:id/pay
payrollRouter.patch('/:id/pay', authMiddleware.isAuthorized, payrollController.markAsPaid);

export default payrollRouter;