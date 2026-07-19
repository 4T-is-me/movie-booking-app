import {LeaveRequest} from "../models/leaveRequestModel.js";
import {Staff} from "../models/staffModel.js";
import {StaffShift} from "../models/staffShiftModel.js";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from 'http-status-codes';
import {constants} from "../utils/Constants.js";
import { Manager } from "../models/managerModel.js";
import mongoose from "mongoose";

const checkStaff = async(accountId) => {
    const currentStaff = await Staff.findOne({accountId})
    if(!currentStaff) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function!");
    }
    return currentStaff
}

const checkShift = async(shiftId) => {
    const shift = await StaffShift.findById(shiftId)
    if(!shift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }
    return shift
}

const checkTheSameCinema = (shiftCinemaId, currentCinemaId, message) => {
    if (shiftCinemaId.toString() !== currentCinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, message || "You can only accept shifts within your assigned cinema!");
    }
}

// Hàm check không được tự nhận ca của chính mình
const checkNotOwnShift = (shiftStaffId, currentStaffId, message) => {
    if (shiftStaffId.toString() === currentStaffId.toString()) {
        throw new ApiError(StatusCodes.CONFLICT, message || "You cannot accept your own shift!");
    }
}

// Hàm check bắt buộc ca đó phải là của mình 
const checkShiftOwner = (shiftStaffId, currentStaffId, message) => {
    if (shiftStaffId.toString() !== currentStaffId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, message || "This shift does not belong to you!");
    }
}


// create leave request
const createLeaveRequest = async(accountId, {staffShiftId, reason}) => {
    
    // check staff
    const staff = await checkStaff(accountId)

    // check shift
    const shift = await checkShift(staffShiftId)

    //check shift belong to staff
    checkShiftOwner(shift.staffId, staff._id,"You can only request time off for your own shifts!")

    //only assigned shift can request
    if(shift.status !== "Assigned") {
        throw new ApiError(StatusCodes.UNAUTHORIZED, "Only assigned shifts can be requested for leave.")
    }

    //check time shift 
    const now = new Date()
    const shiftDate = new Date(shift.workDate)

    //set start shift time 
    const startHour = constants.SHIFT_START_HOURS[shift.shift] || 0;
    const shiftStartTime = new Date(shiftDate);
    shiftStartTime.setUTCHours(startHour, 0, 0, 0);

    //reject if start time <= now
    if(shiftStartTime <= now) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, "Cannot request leave for a past or currently active shift!")
    }

    //check duplicate leave request
    const existingLeaveRequest = await LeaveRequest.findOne({
        staffShiftId,
        status: { $in: ["Pending", "Approved"] }
    })
    
    if(existingLeaveRequest) {
        throw new ApiError(StatusCodes.CONFLICT, "You have already requested leave for this shift!")
    }

    //create leaveRequest
    const leaveRequest = await LeaveRequest.create({
        staffId: staff._id,
        staffShiftId,
        reason,
        status: "Pending"
    })
    return {
        leaveRequestId: leaveRequest._id,
        status: leaveRequest.status
    };
}

// get replacement shifts - staff
const getReplacementShifts = async (accountId) => {
    
     const currentStaff = await checkStaff(accountId)

    const today = new Date()
    today.setUTCHours(0, 0, 0, 0)

    const approvedRequests = await LeaveRequest.find({ status: "Approved" })
    .populate({
        path: "staffId",
        select: "fullName phone position"
    })
    .populate({
        path: "staffShiftId",
        match:{
            cinemaId: currentStaff.cinemaId, // the same cinema
            staffId: {$ne: currentStaff._id}, //not get my shift
            status: "NeedReplacement", //shift need replacement
            workDate: {$gte: today} //shift in the future
        },
        select: "workDate shift status cinemaId"
    })
    .sort({createdAt: -1})
    .lean()

    // filter requests have null staffShiftId
    const validRequests = approvedRequests.filter((request) => request.staffShiftId !== null);

    return validRequests.map((item) => ({
        leaveRequestId: item._id,
        shiftId: item.staffShiftId._id,
        staffName: item.staffId?.fullName || "N/A",
        staffPosition: item.staffId?.position || "Staff",
        reason: item.reason,
        workDate: item.staffShiftId.workDate,
        shift: item.staffShiftId.shift,
        approvedAt: item.approvedAt
    }));
}

// accept replacement shift - staff
const acceptReplacementShift = async(accountId, shiftId) => {
    //check staff
    const currentStaff = await checkStaff(accountId)

    //find shift
    const shift = await checkShift(shiftId)

    //check shift status
    if(shift.status !== "NeedReplacement") {
        throw new ApiError(StatusCodes.CONFLICT, "This shift has already been assigned or is no longer available!")
    }
    // not accept my shift
    checkNotOwnShift(shift.staffId, currentStaff._id, "You cannot accept your own shift!");
    checkTheSameCinema(shift.cinemaId, currentStaff.cinemaId, "You can only accept shifts within your assigned cinema!");

    const workDateObj = new Date(shift.workDate)
    const startOfDay = new Date(workDateObj.setUTCHours(0, 0, 0, 0))
    const endOfDay = new Date(workDateObj.setUTCHours(23, 59, 59, 999))

    // check duplicate shift
    const existingShift = await StaffShift.findOne({
        staffId: currentStaff._id,
        workDate: { $gte: startOfDay, $lte: endOfDay },
        shift: shift.shift

    })

    if(existingShift) {
        throw new ApiError(StatusCodes.CONFLICT, `You already have a '${shift.shift}' shift on this date.`)
    }

    // check limit shift in a day
    const dailyShiftCount = await StaffShift.countDocuments({
        staffId: currentStaff._id,
        workDate: { $gte: startOfDay, $lte: endOfDay }
    })
    if(dailyShiftCount >= constants.MAX_SHIFT_LEAVE_PER_DAY) {
        throw new ApiError(StatusCodes.CONFLICT, "You cannot work more than 3 shifts in a single day.")
    }

    //check B has leaveRequest approved the same shift in a day
    const approvedRequests = await LeaveRequest.find({
        staffId: currentStaff._id,
        status: "Approved"
    }).populate("staffShiftId")

    const isBOnApprovedLeave = approvedRequests.some(leave => {
        const s = leave.staffShiftId
        if(!s) return false
        const isSameShift = new Date(s.workDate).getTime() === startOfDay.getTime()
        return isSameShift && s.shift === shift.shift
    })

    if(isBOnApprovedLeave) {
        throw new ApiError(StatusCodes.CONFLICT, "You cannot accept a shift on a time block where you have an approved leave!")
    }

    //Transaction avoid Race Condition
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
        const updatedShift = await StaffShift.findOneAndUpdate(
            {
            _id: shiftId,
            status: "NeedReplacement"
            },
            {
                $set: {
                    staffId: currentStaff._id,
                    status: "Assigned",
                    originalStaffId: shift.originalStaffId || shift.staffId
                }
            },
            {new: true, session}
        )
        //updatedShift === null => other staff accept this shift
        if(!updatedShift) {
            throw new ApiError(StatusCodes.CONFLICT, "This shift has already been taken by another staff member!")
        }
        await session.commitTransaction();
        return { success: true, data: updatedShift };
        
    } catch (error) {
        session.abortTransaction();
        throw error
    } finally {
        await session.endSession();
    }
}


const getStaffShifts = async (accountId, { startDate, endDate }) => {
    const staff = await checkStaff(accountId);
    
    const filter = {
        staffId: staff._id
    };
    
    if (startDate && endDate) {
        const start = new Date(startDate);
        start.setUTCHours(0, 0, 0, 0);

        const end = new Date(endDate);
        end.setUTCHours(23, 59, 59, 999);

        filter.workDate = {
            $gte: start,
            $lte: end
        };
    }
    
    const shifts = await StaffShift.find(filter)
        .populate({
            path: 'originalStaffId',
            select: 'fullName'
        })
        .sort({ workDate: 1 });
        
    return shifts.map(item => ({
        shiftId: item._id,
        staffId: item.staffId,
        cinemaId: item.cinemaId,
        workDate: item.workDate,
        position: staff.position,
        shift: item.shift,
        status: item.status,
        originalStaffName: item.originalStaffId?.fullName || null
    }));
}

const getStaffLeaveRequests = async (accountId, statusFilter, page = 1, limit = 10) => {
    const staff = await checkStaff(accountId);
    const filter = { staffId: staff._id };
    if (statusFilter) {
        filter.status = statusFilter;
    }

    // Tính toán số lượng bỏ qua
    const skip = (parseInt(page) - 1) * parseInt(limit);

    // Chạy song song cả đếm tổng và lấy data cho nhanh
    const [totalItems, leaveRequests] = await Promise.all([
        LeaveRequest.countDocuments(filter),
        LeaveRequest.find(filter)
            .populate({
                path: 'staffShiftId',
                select: 'workDate shift'
            })
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(parseInt(limit))
    ]);

    const totalPages = Math.ceil(totalItems / limit);

    const mappedData = leaveRequests.map(item => ({
        leaveRequestId: item._id,
        shiftId: item.staffShiftId?._id || null,
        workDate: item.staffShiftId?.workDate || null,
        shift: item.staffShiftId?.shift || null,
        reason: item.reason,
        status: item.status,
        rejectReason: item.rejectReason,
        createdAt: item.createdAt
    }));   

    // Trả thêm thông tin phân trang ra ngoài
    return {
        docs: mappedData,
        totalItems,
        totalPages,
        currentPage: parseInt(page),
        limit: parseInt(limit)
    };
}

const updatePendingReason = async(accountId, leaveRequestId, reason) => {
    const staff = await checkStaff(accountId);
    const leaveRequest = await LeaveRequest.findById(leaveRequestId)
    if(!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found!");
    }
    const checkLeaveRequest = await checkShiftOwner(staff._id, leaveRequest.staffId, "You are not authorized to edit this request.")
    
    //check status
    if(leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, "Only pending leave requests can be edited.");
    }
    leaveRequest.reason = reason;

    await leaveRequest.save();

    return { 
        success: true, 
        message: "Leave request updated successfully." 
    };
}

const deletePendingRequest = async(accountId, leaveRequestId) => {
    const staff = await checkStaff(accountId);
    const leaveRequest = await LeaveRequest.findById(leaveRequestId)
    if(!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found!");
    }
    const checkLeaveRequest = await checkShiftOwner(staff._id, leaveRequest.staffId, "You are not authorized to edit this request.")
    //check status
    if(leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, "Only pending leave requests can be edited.");
    }
    await LeaveRequest.findByIdAndDelete(leaveRequestId);    

    return { 
        success: true, 
        message: "Leave request deleted successfully." 
    };
}

export const StaffLeaveRequestService = {
    createLeaveRequest,
    getReplacementShifts,
    acceptReplacementShift,
    getStaffShifts,
    getStaffLeaveRequests,
    updatePendingReason,
    deletePendingRequest
}
