import { ManagerStaffShiftService } from "../services/managerStaffShiftService.js";
import { StatusCodes } from "http-status-codes";


const getShifts = async (req, res, next) => {
  try {
    const accountId = req.user.accountId;
    // get query params (ex: startDate, endDate, shift, status)
    const filter = req.query;
    const result = await ManagerStaffShiftService.getShifts(accountId, filter);

    return res.status(StatusCodes.OK).json({
      success: true,
      message: "Successfully get staff shifts.",
      data: result
    });
    
  } catch (error) {
    next(error);
  }
}


const updateShifts = async (req, res, next) => {
  try {
      const accountId = req.user.accountId; 
      const { id } = req.params;
      const { staffId, shift, status } = req.body;

      const result = await ManagerStaffShiftService.updateShifts(accountId, id, {
          staffId,
          shift,
          status
        });

      return res.status(StatusCodes.OK).json({
          success: true,
          message: "Update shift successfully.",
          data: result
      });
  } catch (error) {
    next(error);
  }
}

// Get staff list for manager's cinema (used by FE algorithm)
const getStaffForManager = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const result = await ManagerStaffShiftService.getStaffForManager(accountId);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully retrieved staff list.",
            data: result
        });
    } catch (error) {
        next(error);
    }
};

// Generate staff shifts (receives pre-computed array from FE algorithm)
const generateShift = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const { dateRange, config, shifts } = req.body;

        // Kiểm tra tính hợp lệ thô của request payload
        if (!shifts || !Array.isArray(shifts) || shifts.length === 0) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "No shift data provided from frontend algorithm."
            });
        }
        const result = await ManagerStaffShiftService.generateShift(accountId, dateRange, config, shifts);

        res.status(StatusCodes.CREATED).json({
            success: true,
            message: result.message,
            data: result
        });

    } catch (error) {
        next(error);
    }
};

// Get staff detail - Manager
export const getStaffDetail = async (req, res, next) => {
  try {
    const accountId = req.user.accountId; 
    const { id } = req.params;

    const result = await ManagerStaffShiftService.getStaffDetailService(accountId, id);

    return res.status(StatusCodes.OK).json({
      statusCode: StatusCodes.OK,
      message: "Successfully retrieved employee details.",
      data: result
    });
  } catch (error) {
    next(error);
  }
};

// Get leave request - Manager
const getLeaveRequestForManager = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const {status} = req.query // Query params: ?status=Pending (Optional)

        const data = await ManagerStaffShiftService.getLeaveRequestForManager(accountId, { status });

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Successfully get leave requests.",
            data
        })
        
    } catch (error) {
        next(error);
    }
}

// Approve leave request - Manager
const approveLeaveRequest = async(req, res, next) => {
    try {
      const accountId = req.user.accountId
      const leaveRequestId = req.params.id

      await ManagerStaffShiftService.approveLeaveRequest(accountId, leaveRequestId)

      return res.status(StatusCodes.OK).json({
        success: true,
        message: "Successfully approve leave request."
      })
      
    } catch (error) {
      next(error);
    }
}
const rejectLeaveRequest = async(req, res, next) => {
  try {
    const accountId = req.user.accountId
    const leaveRequestId = req.params.id
    const { rejectReason } = req.body;

    await ManagerStaffShiftService.rejectLeaveRequest(accountId, leaveRequestId, { rejectReason })
    return res.status(StatusCodes.OK).json({
      success: true,
      message: "Leave request rejected successfully."
    })
  }catch (error) {
    next(error);
  }
}
const undoLeaveRequest = async(req, res, next) => {
  try {
    const accountId = req.user.accountId
    const { id } = req.params;
    const result = await ManagerStaffShiftService.undoLeaveRequest( accountId, id);
    return res.status(StatusCodes.OK).json({
      success: true,
      message: "Undo leave request successfully.",
      data: result
    }) 
  }catch (error) {
    next(error);
  }
}


export const StaffShiftController = {
  getShifts,
  updateShifts,
  getStaffForManager,
  generateShift,
  getStaffDetail,
  getLeaveRequestForManager,
  approveLeaveRequest,
  rejectLeaveRequest,
  undoLeaveRequest
};