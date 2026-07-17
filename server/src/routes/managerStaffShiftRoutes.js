import express from 'express'
import { authMiddleware } from '../middlewares/authMiddleware.js'
import { StaffShiftController } from '../controllers/managerStaffShiftController.js'

const managerStaffShiftRouter = express.Router()

// GET /api/manager/staff-shifts/staff - get list of active staff for manager's cinema
managerStaffShiftRouter.get('/staff', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getStaffForManager)

// GET /api/manager/staff-shifts
managerStaffShiftRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getShifts)

// POST /api/manager/staff-shifts/generate
managerStaffShiftRouter.post('/generate', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.generateShift)
// GET /api/manager/staff-shifts/leave-requests
managerStaffShiftRouter.get('/leave-requests', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getLeaveRequestForManager)

// PUT /api/manager/staff-shifts/:id
managerStaffShiftRouter.put('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.updateShifts)

// GET /api/manager/staff-shifts/:id
managerStaffShiftRouter.get('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getStaffDetail)

// PATCH /api/manager/staff-shifts/leave-requests/:id/approve
managerStaffShiftRouter.patch('/leave-requests/:id/approve', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.approveLeaveRequest)
// PATCH /api/manager/staff-shifts/leave-requests/:id/reject
managerStaffShiftRouter.patch('/leave-requests/:id/reject', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.rejectLeaveRequest)

export default managerStaffShiftRouter