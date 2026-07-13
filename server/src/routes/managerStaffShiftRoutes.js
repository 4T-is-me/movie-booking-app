import express from 'express'
import { authMiddleware } from '../middlewares/authMiddleware.js'
import { StaffShiftController } from '../controllers/managerStaffShiftController.js'

const managerStaffShiftRouter = express.Router()

// GET /api/manager/staff-shifts
managerStaffShiftRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getShifts)

// POST /api/manager/staff-shifts
managerStaffShiftRouter.post('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.createShift)

// POST /api/manager/staff-shifts/generate
managerStaffShiftRouter.post('/generate', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.generateShift)

// PUT /api/manager/staff-shifts/:id
managerStaffShiftRouter.put('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.updateShifts)

// GET /api/manager/staff-shifts/:id
managerStaffShiftRouter.get('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Manager'), StaffShiftController.getStaffDetail)


export default managerStaffShiftRouter