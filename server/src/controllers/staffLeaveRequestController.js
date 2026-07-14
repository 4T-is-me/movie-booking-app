import { StaffLeaveRequestService } from "../services/staffLeaveRequestService.js";

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
        const {shiftId} = req.params;

        const data = await StaffLeaveRequestService.acceptReplacementShift(accountId, shiftId);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully accept replacement shift."
        })
        
    } catch (error) {
        next(error);
    }
}


export const staffLeaveRequestController = {
    createLeaveRequest,
    getReplacementShifts,
    acceptReplacementShift
}