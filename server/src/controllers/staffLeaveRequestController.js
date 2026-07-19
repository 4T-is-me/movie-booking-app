import { StaffLeaveRequestService } from "../services/staffLeaveRequestService.js";
import { StatusCodes } from "http-status-codes";
const createLeaveRequest = async(req, res, next) => {
    try {
        const accountId = req.user.accountId
        const {staffShiftId, reason} = req.body;

        if (!staffShiftId || !reason) {
            return res.status(400).json({
                success: false,
                message: "staffShiftId and reason are required."
            });
        }

        const result = await StaffLeaveRequestService.createLeaveRequest(accountId, {staffShiftId, reason})

        return res.status(StatusCodes.CREATED).json({
            success: true,
            message: "Leave request submitted successfully.",
            data: result
        })
    } catch (error) {
        next(error);
    }
}

const getReplacementShifts = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const data = await StaffLeaveRequestService.getReplacementShifts(accountId);

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully get replacement shifts.",
            data
        })
        
    } catch (error) {
        next(error);
    }
}

const acceptReplacementShift = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const {id} = req.params;

        const data = await StaffLeaveRequestService.acceptReplacementShift(accountId, id);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully accept replacement shift."
        })
        
    } catch (error) {
        next(error);
    }
}

const getStaffShifts = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const filter = req.query; // { startDate, endDate }
        console.log(`[API GET Shifts] Request from accountId: ${accountId}, query:`, filter);
        const data = await StaffLeaveRequestService.getStaffShifts(accountId, filter);
        console.log(`[API GET Shifts] Found ${data ? data.length : 0} shifts for accountId: ${accountId}`);

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully get staff shifts.",
            data
        })
    } catch (error) {
        console.error(`[API GET Shifts] Error:`, error);
        next(error);
    }
}

const getMyLeaveRequests = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const { status, page = 1, limit = 10 } = req.query; 
        
        const result = await StaffLeaveRequestService.getStaffLeaveRequests(accountId, status, page, limit);
        
        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully get leave requests.",
            data: result 
        });
    } catch (error) {
        next(error);
    }
}

const updatePendingReason = async (req, res, next) => {
    try {
        const accountId  = req.user.accountId;
        const { id } = req.params;
        const { reason } = req.body;

        const result = await StaffLeaveRequestService.updatePendingReason(accountId, id, reason);

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Leave request updated successfully.",
        })
    } catch (error) {
        next(error);
    }
}
const deletePendingRequest = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const { id } = req.params;
        
        const result = await StaffLeaveRequestService.deletePendingRequest(accountId, id);

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Leave request deleted successfully.",
        })
        
    } catch (error) {
        next(error);
    }
}

export const staffLeaveRequestController = {
    createLeaveRequest,
    getReplacementShifts,
    acceptReplacementShift,
    getStaffShifts,
    getMyLeaveRequests,
    updatePendingReason,
    deletePendingRequest
}