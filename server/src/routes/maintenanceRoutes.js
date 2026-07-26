import express from 'express';
import { MaintenanceController } from '../controllers/maintenanceController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = express.Router();

// User APIs
router.post('/bookings/:id/resolve-maintenance', authMiddleware.isAuthorized, MaintenanceController.resolveUserAction);

// Manager/Admin APIs (Room & Schedule Maintenance)
router.post('/rooms/:roomId/trigger-maintenance', MaintenanceController.triggerMaintenance);
router.get('/rooms/:roomId/preview-restored-schedule', MaintenanceController.previewRestoredSchedule);
router.post('/schedules/confirm-restored-schedule', MaintenanceController.confirmRestoredSchedule);

export default router;
