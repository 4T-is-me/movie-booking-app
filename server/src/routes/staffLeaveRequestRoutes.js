import { Router } from "express";
import { authMiddleware } from "../middlewares/authMiddleware.js";
import { staffLeaveRequestController } from "../controllers/staffLeaveRequestController.js";

const staffLeaveRequestRouter = Router();

//POST /api/staffs/leave-requests/create
staffLeaveRequestRouter.post("/create", authMiddleware.isAuthorized, staffLeaveRequestController.createLeaveRequest);
// GET /api/staffs/leave-requests/replacement-shifts
staffLeaveRequestRouter.get("/replacement-shifts", authMiddleware.isAuthorized, staffLeaveRequestController.getReplacementShifts)
// POST /api/staffs/leave-requests/:id/accept
staffLeaveRequestRouter.post("/:id/accept", authMiddleware.isAuthorized, staffLeaveRequestController.acceptReplacementShift)
// GET /api/staffs/leave-requests/shifts
staffLeaveRequestRouter.get("/shifts", authMiddleware.isAuthorized, staffLeaveRequestController.getStaffShifts)
export default staffLeaveRequestRouter;