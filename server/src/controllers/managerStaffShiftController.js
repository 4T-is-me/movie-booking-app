import { ManagerStaffShiftService } from "../services/managerStaffShiftService.js";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";


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


const createShift = async (req, res, next) => {
    try {
    const accountId = req.user.accountId; 
    const result = await ManagerStaffShiftService.createShift(accountId, req.body);
    
    return res.status(StatusCodes.CREATED).json({
      success: true,
      message: "Successfully schedule staff shifts.",
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
      const updateData = req.body;

      const result = await ManagerStaffShiftService.updateShifts(accountId, id, updateData);
  } catch (error) {
    next(error);
  }
}

const generateShift = async (req, res, next) => {
  try {
    const accountId = req.user.accountId;
    const dateRange = req.body; // { startDate: "...", endDate: "..." }
    const result = await ManagerStaffShiftService.generateShift(accountId, dateRange);
    return res.status(StatusCodes.CREATED).json({
      success: true,
      message: "Successfully generate staff shifts.",
      data: result
    })
  } catch (error) {
    next(error);
  }
}
const deleteShift = async (req, res, next) => {
  try {
      const accountId = req.user.accountIds; 
      const { id } = req.params;
      const result = await ManagerStaffShiftService.deleteShift(accountId, id);
  } catch (error) {
    next(error);
  }
}


export const StaffShiftController = {
  getShifts,
  createShift,
  updateShifts,
  generateShift,
  deleteShift
};