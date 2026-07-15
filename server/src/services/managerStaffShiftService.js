import { Manager } from "../models/managerModel.js";
import { Staff } from "../models/staffModel.js";
import { StaffShift } from "../models/staffShiftModel.js";
import { LeaveRequest } from "../models/leaveRequestModel.js";
import { StatusCodes } from "http-status-codes";
import { constants } from "../utils/Constants.js";
// import { LeaveRequest } from "../models/leaveRequestModel.js";
import ApiError from "../utils/ApiError.js";
import { formatDate } from "../utils/formatters.js";
import mongoose from "mongoose";

const checkManager = async (accountId) => {
    const manager = await Manager.findOne({ accountId })
    if (!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
    }
    return manager;
}

const checkCinemaPermission = (managerCinemaId, objectCinemaId) => {
    if (objectCinemaId.toString() !== managerCinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only perform actions for staff members or shifts belonging to your own cinema.");
    }
}



const getShifts = async (accountId, query) => {
    const { staffId, workDate, startDate, endDate, shift } = query;

    const manager = await checkManager(accountId);

    const filter = {
        cinemaId: manager.cinemaId,
    }
    if (staffId) {
        filter.staffId = staffId;
    }
    if (shift) {
        filter.shift = shift;
    }
    if (startDate && endDate) {
        const start = new Date(startDate);
        start.setUTCHours(0, 0, 0, 0);

        const end = new Date(endDate);
        end.setUTCHours(23, 59, 59, 999); // Lấy hết thời gian của ngày kết thúc

        filter.workDate = {
            $gte: start,
            $lte: end
        };
    }

    const shifts = await StaffShift.find(filter)
        .populate({
            path: 'staffId',
            select: 'fullName position phone rating kpiPoints'
        })
        .sort({
            workDate: 1
        })

    const shiftOrder = {
        Morning: 1,
        Afternoon: 2,
        Evening: 3,
        Night: 4
    };

    // Sắp xếp lại danh sách: Ưu tiên trạng thái trước, rồi mới tới ngày và ca làm
    shifts.sort((a, b) => {

        const isACompleted = a.status === "Completed" ? 1 : 0;
        const isBCompleted = b.status === "Completed" ? 1 : 0;

        if (isACompleted !== isBCompleted) {
            return isACompleted - isBCompleted;
        }

        const dateA = new Date(a.workDate).getTime();
        const dateB = new Date(b.workDate).getTime();
        if (dateA !== dateB) {
            return dateA - dateB;
        }

        const orderA = shiftOrder[a.shift] || 99;
        const orderB = shiftOrder[b.shift] || 99;
        return orderA - orderB;
    });

    return shifts.map(item => ({
        shiftId: item._id,
        staffId: item.staffId?._id,
        cinemaId: item.cinemaId,
        staffName: item.staffId?.fullName || "",
        position: item.staffId?.position || "",
        phone: item.staffId?.phone || "",
        workDate: item.workDate,
        shift: item.shift,
        status: item.status,
        rating: item.staffId?.rating || 0,
        kpiPoints: item.staffId?.kpiPoints || 0
    }))
}


//unfinished
const updateShifts = async (accountId, shiftId, updateData) => {
    console.log(updateData);
    const { staffId, shift, status } = updateData;
    console.log(shiftId);

    const manager = await checkManager(accountId);

    const currentShift = await StaffShift.findById(shiftId);
    if (!currentShift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }

    if (currentShift.cinemaId.toString() !== manager.cinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only update shifts for staff members belonging to your own cinema.");
    }
    checkCinemaPermission(manager.cinemaId, currentShift.cinemaId);

    const fixedWorkDate = currentShift.workDate;

    let newStaffId = currentShift.staffId
    let newShift = currentShift.shift

    //Nếu có thay đổi Nhân viên
    if (staffId && staffId !== currentShift.staffId.toString()) {
        const staff = await Staff.findById(staffId);
        if (!staff) {
            throw new ApiError(StatusCodes.NOT_FOUND, "The staff does not exist.");
        }

        if (staff.cinemaId.toString() !== manager.cinemaId.toString()) {
            throw new ApiError(StatusCodes.FORBIDDEN, "You can only assign staff members belonging to your own cinema.");
        }
        newStaffId = staff._id;
    }

    //update shift
    if (shift) {
        newShift = shift;
    }

    //Validate nếu có sự thay đổi về Người hoặc Ca
    if (staffId || shift) {

        // Check: Kiểm tra trùng lặp (Nhân viên đã làm ca này trong ngày này chưa)
        const duplicate = await StaffShift.findOne({
            _id: { $ne: shiftId },
            staffId: newStaffId,
            workDate: fixedWorkDate,
            shift: newShift
        });
        if (duplicate) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has already been assigned this shift for the day.");
        }

        // Check: Kiểm tra lịch nghỉ phép (Theo đúng cặp ngày + ca mới)
        const approvedLeave = await LeaveRequest.findOne({
            staffId: newStaffId,
            workDate: fixedWorkDate,
            shift: newShift,
            status: "Approved"
        });
        if (approvedLeave) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has been approved for leave on this specific shift.");
        }

        // Check: Kiểm tra giới hạn ca làm trong ngày (Tối đa 2 ca/ngày)
        const currentShiftsCount = await StaffShift.countDocuments({
            _id: { $ne: shiftId },
            staffId: newStaffId,
            workDate: fixedWorkDate
        });
        if (currentShiftsCount >= 2) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "This staff worked a maximum of two shifts on this day.");
        }
    }
    //update
    currentShift.staffId = newStaffId;
    currentShift.shift = newShift;

    if (status) {
        currentShift.status = status;
    }
    await currentShift.save();
    const populatedShift = await StaffShift.findById(currentShift._id)
        .populate({
            path: 'staffId',
            select: 'fullName position phone'
        })
        .lean();

    return {
        shiftId: populatedShift._id,
        staffId: populatedShift.staffId?._id,
        fullName: populatedShift.staffId?.fullName || "",
        position: populatedShift.staffId?.position || "",
        phone: populatedShift.staffId?.phone || "",
        workDate: populatedShift.workDate.toISOString().split('T')[0],
        shift: populatedShift.shift,
        status: populatedShift.status
    };
}



//aggregate current shifts for the day at the cinema
const getExistingShiftsOfDay = async (cinemaId, workDate) => {
    const exsitingShifts = await StaffShift.find({
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        workDate: workDate
    }).lean();

    const shiftOccupancy = { Morning: [], Afternoon: [], Evening: [], Night: [] };
    const staffDailyCount = {}

    exsitingShifts.forEach(shift => {
        if (shiftOccupancy[shift.shift]) {
            shiftOccupancy[shift.shift].push(shift.staffId.toString());
        }
        const staffIdStr = shift.staffId.toString();
        staffDailyCount[staffIdStr] = (staffDailyCount[staffIdStr] || 0) + 1
    })

    return {
        shiftOccupancy,
        staffDailyCount
    }
}


// Filter leave list based on conditions (cinema) when populating staff
const getAvailableStaffForDay = async (allStaffs, cinemaId, workDate) => {
    // chỉ tìm những leave request status = approved
    const approvedLeaves = await LeaveRequest.find({
        status: "Approved",
        fromDate: { $lte: workDate },
        toDate: { $gte: workDate }
    })
        .populate({
            path: "staffId",
            select: "cinemaId"
        })
        .lean();
    // Filter cinema staff currently requesting time off
    const absentStaffIds = approvedLeaves.filter(leave => leave.staffId.cinemaId.toString() === cinemaId.toString()
    ).map(leave => leave.staffId._id.toString());

    // List of staff working that day
    return allStaffs.filter(staff => !absentStaffIds.includes(staff._id.toString()));
}

//  
const pickNextStaffRoundRobin = (dayStaffs, currentShift, shiftOccupancy, staffDailyCount, context) => {
    // Define the table for the immediately preceding shift
    const previousShiftMap = {
        'Afternoon': 'Morning',
        'Night': 'Evening'
    };

    const previousShift = previousShiftMap[currentShift];

    //ưu tiên ca liền kề vd Nếu ca hiện tại có ca trước (như Afternoon có Morning, Night có Evening)
    if (previousShift && shiftOccupancy[previousShift]) {
        // Tìm những người ĐÃ LÀM ca trước đó và CÒN SLOT làm ca hôm nay (< MAX_SHIFT_PER_DAY)
        const candidatesFromPrevShift = shiftOccupancy[previousShift].filter(staffId => {
            const count = staffDailyCount[staffId] || 0;
            const isAlreadyInCurrentShift = shiftOccupancy[currentShift].includes(staffId);
            return count < constants.MAX_SHIFT_PER_DAY && !isAlreadyInCurrentShift;
        });

        // Nếu tìm thấy người thỏa mãn -> Bốc ngay người này!
        if (candidatesFromPrevShift.length > 0) {
            // Lấy người đầu tiên trong danh sách ứng viên liền kề
            const selectedId = candidatesFromPrevShift[0];
            return selectedId;
        }
    }

    //ROUND ROBIN CHUẨN
    const totalStaff = dayStaffs.length;
    if (totalStaff === 0) {
        return null;
    }
    for (let i = 0; i < totalStaff; i++) {
        const currentIndex = (context.staffIndex + i) % totalStaff;
        const staff = dayStaffs[currentIndex];
        const staffIdStr = staff._id.toString();

        const alreadyInshift = shiftOccupancy[currentShift].includes(staffIdStr);
        const currentCount = staffDailyCount[staffIdStr] || 0;

        if (!alreadyInshift && currentCount < constants.MAX_SHIFT_PER_DAY) {
            context.staffIndex = (currentIndex + 1) % totalStaff;
            return staffIdStr;
        }
    }
    return null;
}

const generateShift = async (accountId, dateRange) => {
    const { startDate, endDate } = dateRange;

    // 1. Chuẩn hóa ngày giờ về UTC 00:00:00
    const currentDate = new Date(startDate);
    currentDate.setUTCHours(0, 0, 0, 0);
    const finalDate = new Date(endDate);
    finalDate.setUTCHours(0, 0, 0, 0);

    if (currentDate > finalDate) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Start date cannot be later than end date.");
    }

    // 2. Bắt buộc tối thiểu 7 ngày
    const diffTime = finalDate.getTime() - currentDate.getTime();
    const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24)) + 1;
    if (diffDays < 7) {
        throw new ApiError(
            StatusCodes.BAD_REQUEST,
            `Minimum schedule range requirement not met. You selected ${diffDays} day(s), but at least 7 days are required.`
        );
    }

    const today = new Date();
    today.setUTCHours(0, 0, 0, 0);
    if (currentDate < today) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Cannot schedule shifts for past dates.");
    }

    // 3. Check Manager & Cinema
    const manager = await checkManager(accountId);

    const cinemaId = manager.cinemaId;

    const allStaff = await Staff.find({ cinemaId }).lean();
    if (!allStaff || allStaff.length === 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "The cinema currently has no staff to schedule.");
    }

    // 4. Lấy tổng ca làm từ đầu tháng trong DB để cân bằng dài hạn
    const startOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
    const existingShiftsCount = await StaffShift.aggregate([
        {
            $match: {
                cinemaId: cinemaId,
                workDate: { $gte: startOfMonth, $lt: currentDate },
                status: { $ne: 'Cancelled' }
            }
        },
        {
            $group: {
                _id: "$staffId",
                total: { $sum: 1 }
            }
        }
    ]);

    const overallStaffCount = {};
    const weeklyWorkDaysCount = {}; // Đếm số ngày đi làm trong tuần

    allStaff.forEach(s => {
        const idStr = s._id.toString();
        const found = existingShiftsCount.find(item => item._id.toString() === idStr);
        overallStaffCount[idStr] = found ? found.total : 0;
        weeklyWorkDaysCount[idStr] = 0;
    });

    const newShiftsToInsert = [];

    // Các cặp ca cố định (Double Shift)
    const SHIFT_PAIRS = [
        { name: 'Morning - Afternoon', shifts: ['Morning', 'Afternoon'] },
        { name: 'Evening - Night', shifts: ['Evening', 'Night'] }
    ];

    let dayIndex = 0;

    // 5. Vòng lặp chia ca theo ngày
    while (currentDate <= finalDate) {
        const targetDate = new Date(currentDate);
        targetDate.setUTCHours(0, 0, 0, 0);

        // Reset số ngày làm khi sang tuần mới (mỗi 7 ngày)
        if (dayIndex > 0 && dayIndex % 7 === 0) {
            Object.keys(weeklyWorkDaysCount).forEach(id => weeklyWorkDaysCount[id] = 0);
        }

        const dayStaff = await getAvailableStaffForDay(allStaff, cinemaId, targetDate);

        const { shiftOccupancy, staffDailyCount } = await getExistingShiftsOfDay(cinemaId, targetDate);

        Object.keys(staffDailyCount).forEach(staffId => {
            overallStaffCount[staffId] = (overallStaffCount[staffId] || 0) + staffDailyCount[staffId];
            if (staffDailyCount[staffId] > 0) {
                weeklyWorkDaysCount[staffId] = (weeklyWorkDaysCount[staffId] || 0) + 1;
            }
        });

        const isToday = targetDate.getTime() === today.getTime();
        const initialStatus = isToday ? 'Active' : 'Assigned';

        // -------------------------------------------------------------
        // XẾP CẶP CA - KHÓA CỨNG ĐIỀU KIỆN NGHỈ 1 NGÀY/TUẦN
        // -------------------------------------------------------------
        for (const pair of SHIFT_PAIRS) {
            const [shift1, shift2] = pair.shifts;

            const occ1 = (shiftOccupancy[shift1] || []).length;
            const occ2 = (shiftOccupancy[shift2] || []).length;

            const neededPairs = Math.min(
                constants.STAFF_PER_SHIFT - occ1,
                constants.STAFF_PER_SHIFT - occ2
            );

            if (neededPairs <= 0) continue;

            for (let p = 0; p < neededPairs; p++) {
                //  Bắt buộc chưa làm ca nào hôm nay VÀ chưa đi làm quá 5 ngày (để chắc chắn có 1 ngày nghỉ)
                const eligibleForPair = dayStaff.filter(staff => {
                    const id = staff._id.toString();
                    const dailyCount = staffDailyCount[id] || 0;
                    const workedDays = weeklyWorkDaysCount[id] || 0;

                    const isFree1 = !(shiftOccupancy[shift1] || []).includes(id);
                    const isFree2 = !(shiftOccupancy[shift2] || []).includes(id);

                    // Khóa cứng: workedDays < 6 (Chỉ cho phép làm tối đa 6 ngày)
                    return dailyCount === 0 && isFree1 && isFree2 && workedDays < 6;
                });

                if (eligibleForPair.length === 0) {
                    const formattedDate = targetDate.toLocaleDateString('vi-VN');
                    throw new ApiError(
                        StatusCodes.BAD_REQUEST,
                        `Allocation error: Not enough available staff to form pair [${pair.name}] on ${formattedDate} (Mandatory 1-day weekly off policy).`
                    );
                }

                //SẮP XẾP: Ưu tiên nhân viên có số ngày đi làm (workedDays) ít nhất
                eligibleForPair.sort((a, b) => {
                    const idA = a._id.toString();
                    const idB = b._id.toString();

                    const daysA = weeklyWorkDaysCount[idA] || 0;
                    const daysB = weeklyWorkDaysCount[idB] || 0;

                    if (daysA !== daysB) return daysA - daysB;

                    const totalA = overallStaffCount[idA] || 0;
                    const totalB = overallStaffCount[idB] || 0;
                    if (totalA !== totalB) return totalA - totalB;

                    return Math.random() - 0.5;
                });

                const selected = eligibleForPair[0];
                const id = selected._id.toString();

                // Gán cặp ca
                [shift1, shift2].forEach(sName => {
                    if (!shiftOccupancy[sName]) shiftOccupancy[sName] = [];
                    shiftOccupancy[sName].push(id);

                    newShiftsToInsert.push({
                        staffId: selected._id,
                        cinemaId: cinemaId,
                        workDate: targetDate,
                        shift: sName,
                        status: initialStatus
                    });
                });

                // Cập nhật bộ đếm
                if ((staffDailyCount[id] || 0) === 0) {
                    weeklyWorkDaysCount[id] = (weeklyWorkDaysCount[id] || 0) + 1;
                }
                staffDailyCount[id] = (staffDailyCount[id] || 0) + 2;
                overallStaffCount[id] = (overallStaffCount[id] || 0) + 2;
            }
        }

        currentDate.setUTCDate(currentDate.getUTCDate() + 1);
        dayIndex++;
    }

    // Lưu Database
    if (newShiftsToInsert.length === 0) {
        return {
            generated: 0,
            startDate,
            endDate,
            message: `Shifts for the selected period (${startDate} to ${endDate}) have already been fully scheduled.`
        };
    }

    const docs = await StaffShift.insertMany(newShiftsToInsert);

    return {
        generated: docs.length,
        startDate,
        endDate,
        message: `Successfully allocated ${docs.length} new shifts from ${startDate} to ${endDate}.`
    };
};
const getWeekRange = (inputDate) => {
    // Ensure we have a valid Date object
    let date = new Date(inputDate);

    // Defensive check if input is invalid or undefined
    if (!inputDate || isNaN(date.getTime())) {
        date = new Date();
    }

    // Clone date object to prevent side-effects
    const utcDate = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));

    // UTC Day: 0 (Sun), 1 (Mon), ..., 6 (Sat)
    const dayOfWeek = utcDate.getUTCDay();
    const distanceToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;

    const monday = new Date(utcDate);
    monday.setUTCDate(utcDate.getUTCDate() - distanceToMonday);

    const sunday = new Date(monday);
    sunday.setUTCDate(monday.getUTCDate() + 6);

    return { monday, sunday };
};
const formatDateString = (dateInput) => {
    if (!dateInput) return '';
    const d = new Date(dateInput);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
};


const getStaffDetailService = async (accountId, shiftId) => {
    //check manager
    const manager = await checkManager(accountId);
    const targetShift = await StaffShift.findById(shiftId)
        .populate({
            path: 'staffId',
            populate: { path: 'accountId', select: 'status' } // get status from account
        })
        .lean();

    if (!targetShift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift information not found.");
    }

    // Check xem ca này có thuộc Cinema của Manager không
    checkCinemaPermission(manager.cinemaId, targetShift.cinemaId);

    const staff = targetShift.staffId;
    if (!staff) {
        throw new ApiError(StatusCodes.NOT_FOUND, "There is no staff member assigned to this shift yet.");
    }

    const shiftDate = new Date(targetShift.workDate);
    const { monday, sunday } = getWeekRange(shiftDate);

    const shiftsInWeek = await StaffShift.find({
        staffId: staff._id,
        workDate: { $gte: monday, $lte: sunday }
    }).lean();


    // Map data 7 day
    const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const weekSchedule = daysOfWeek.map((dayName, index) => {
        const currentDate = new Date(monday);
        currentDate.setUTCDate(monday.getUTCDate() + index);
        const dateStr = formatDateString(currentDate);
        const shiftOrder = {
            Morning: 1,
            Afternoon: 2,
            Evening: 3,
            Night: 4
        };

        shiftsInWeek.sort((a, b) => {
            if (a.workDate.getTime() !== b.workDate.getTime()) {
                return a.workDate - b.workDate;
            }

            return shiftOrder[a.shift] - shiftOrder[b.shift];
        });


        const matchedShifts = shiftsInWeek.filter(
            s => formatDateString(new Date(s.workDate)) === dateStr
        );
        const shiftNames = matchedShifts.map(s => s.shift);

        return {
            day: dayName,
            date: dateStr,
            shift: shiftNames.length > 0 ? shiftNames.join(" - ") : null
        };
    });

    //cal Attendance Rate
    const [completedCount, absentCount] = await Promise.all([
        StaffShift.countDocuments({ staffId: staff._id, status: 'Completed' }),
        StaffShift.countDocuments({ staffId: staff._id, status: 'Absent' })
    ]);

    const totalEvaluated = completedCount + absentCount;
    const attendanceRate = totalEvaluated > 0
        ? Number(((completedCount / totalEvaluated) * 100).toFixed(1))
        : 100.0; // Defaults to 100% if no shifts have ended yet

    return {
        // Information on the current shift
        shiftId: targetShift._id,
        workDate: formatDateString(new Date(targetShift.workDate)),
        shift: targetShift.shift,
        shiftStatus: targetShift.status,

        // Information about the employee working that shift
        staffInfo: {
            staffId: staff._id,
            fullName: staff.fullName,
            position: staff.position,
            phone: staff.phone,
            status: staff.accountId?.status === 1 ? 'Active' : 'Inactive',
            rating: staff.rating || 0,
            kpiPoints: staff.kpiPoints || 0,
            ticketSold: staff.ticketSold || 0,
            attendanceRate
        },

        //That employee's weekly schedule
        weekSchedule
    };
}
const getLeaveRequestForManager = async (accountId, { status }) => {
    const manager = await checkManager(accountId);
    const cinemaId = manager.cinemaId

    //Dựng filter cơ bản (nếu client truyền status: Pending, Approved, Rejected)
    const filter = {}
    if (status) {
        filter.status = status
    }

    //query + populate correct cinemaId of manager
    const leaveRequests = await LeaveRequest.find(filter)
        .populate({
            path: "staffId",
            select: "fullname phone position"
        })
        .populate({
            path: "staffShiftId",
            match: { cinemaId: cinemaId },
            select: "workDate shift status cinemaId"
        })
        .sort({ createdAt: -1 })
        .lean()

    // filter requests have null staffShiftId(because not belong to cinema of manager)
    const filteredRequests = leaveRequests.filter(request => request.staffShiftId !== null)

    //Sort: Pending -> Approved -> Rejected (createdAt DESC in every group)
    const statusPriority = { Pending: 1, Approved: 2, Rejected: 3 };

    filteredRequests.sort((a, b) => {
        const priorityA = statusPriority[a.status] || 99;
        const priorityB = statusPriority[b.status] || 99;

        if (priorityA !== priorityB) {
            return priorityA - priorityB; // Pending display first
        }
        // the same status => display newest first
        return new Date(b.createdAt) - new Date(a.createdAt);
    });
    return filteredRequests.map(item => ({
        leaveRequestId: item._id,
        staffId: item.staffId?._id,
        staffName: item.staffId?.fullName || "N/A",
        staffPosition: item.staffId?.position || "Staff",
        staffShiftId: item.staffShiftId?._id,
        shift: item.staffShiftId?.shift,
        workDate: formatDate(item.staffShiftId?.workDate),
        reason: item.reason,
        status: item.status,
        createdAt: item.createdAt
    }));

}


const approveLeaveRequest = async (accountId, leaveRequestId) => {
    const manager = await checkManager(accountId);

    const leaveRequest = await LeaveRequest.findById(leaveRequestId).populate("staffShiftId");
    if (!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found");
    }
    const shift = leaveRequest.staffShiftId
    if (!shift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }
    //Check cinema permission
    checkCinemaPermission(manager.cinemaId, shift.cinemaId);
    //Check  LeaveRequest status(only approve pending request)
    if (leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, `Cannot approve a request with status '${leaveRequest.status}'!`);
    }

    //check staffShift status
    if (shift.status !== "Assigned") {
        throw new ApiError(StatusCodes.CONFLICT, `Shift is already in '${shift.status}' status.`);
    }

    //transaction
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
        //update leaveRequest status
        leaveRequest.status = "Approved";
        leaveRequest.managerId = manager._id;
        leaveRequest.approvedAt = new Date();
        await leaveRequest.save({ session });

        //update leaveRequest
        shift.status = "NeedReplacement";
        await shift.save({ session });

        //commit Transaction
        await session.commitTransaction();

    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        // close session
        session.endSession();
    }

}
//reject leaveRequest - manager
const rejectLeaveRequest = async (accountId, leaveRequestId, rejectReason) => {

    const manager = await checkManager(accountId);
    const leaveRequest = await LeaveRequest.findById(leaveRequestId).populate("staffShiftId");
    if (!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found!");
    }

    const shift = leaveRequest.staffShiftId
    if (!shift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Associated shift not found!");
    }
    //check permission
    checkCinemaPermission(manager.cinemaId, shift.cinemaId);
    //check status
    if (leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, `This leave request has already been ${leaveRequest.status.toLowerCase()}!`);
    }

    leaveRequest.status = "Rejected";
    leaveRequest.managerId = manager._id;
    leaveRequest.rejectReason = rejectReason || "No specific reason provided.";
    leaveRequest.approvedAt = new Date();
    await leaveRequest.save();

    return { success: true };
}


export const ManagerStaffShiftService = {
    updateShifts,
    generateShift,
    getShifts,
    getStaffDetailService,
    getLeaveRequestForManager,
    approveLeaveRequest
};
