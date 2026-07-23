import { Manager } from "../models/managerModel.js";
import { Staff } from "../models/staffModel.js";
import { StaffShift } from "../models/staffShiftModel.js";
import { LeaveRequest } from "../models/leaveRequestModel.js";
import { StatusCodes } from "http-status-codes";
import { constants } from "../utils/Constants.js";
import {NotificationService} from "./notificationService.js";
import { formatDate } from "../utils/formatters.js";
import ApiError from "../utils/ApiError.js";
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
    const { staffId, workDate, startDate, endDate, shift, status } = query;

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
    if (status) {
        filter.status = status;
    }

    // Khởi tạo object điều kiện cho workDate
    const dateFilter = {};

    if (startDate) {
        const start = new Date(startDate);
        start.setUTCHours(0, 0, 0, 0);
        dateFilter.$gte = start; // Lấy từ 00:00:00 của startDate trở đi
    }

    if (endDate) {
        const end = new Date(endDate);
        end.setUTCHours(23, 59, 59, 999);
        dateFilter.$lte = end; // Lấy đến 23:59:59 của endDate trở về trước
    }

    // Nếu người dùng KHÔNG truyền cả startDate lẫn endDate, mặc định lấy trọn ngày hôm nay
    if (!startDate && !endDate) {
        const startToday = new Date();
        startToday.setUTCHours(0, 0, 0, 0);

        const endToday = new Date();
        endToday.setUTCHours(23, 59, 59, 999);

        dateFilter.$gte = startToday;
        dateFilter.$lte = endToday;
    }

    filter.workDate = dateFilter;

    const shifts = await StaffShift.find(filter)
        .populate({
            path: 'staffId',
            select: 'fullName position phone rating kpiPoints'
        })
        .sort({
            workDate: 1
        });

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
    }));

}
const updateShifts = async (accountId, shiftId, updateData) => {
    const { staffId, shift, status } = updateData;

    const manager = await checkManager(accountId);

    // 1. Kiểm tra sự tồn tại của ca làm
    const currentShift = await StaffShift.findById(shiftId);
    if (!currentShift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }

    // 2. Chặn chỉnh sửa nếu ca đã Completed hoặc Absent
    if (["Completed", "Absent"].includes(currentShift.status)) {
        throw new ApiError(
            StatusCodes.CONFLICT, 
            `Shifts with status '${currentShift.status}' can no longer be edited.`
        );
    }

    // 3. Kiểm tra quyền quản lý theo Cinema
    if (currentShift.cinemaId.toString() !== manager.cinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only update shifts for staff members belonging to your own cinema.");
    }
    checkCinemaPermission(manager.cinemaId, currentShift.cinemaId);

    const fixedWorkDate = currentShift.workDate;
    let newStaffId = currentShift.staffId;
    let newShift = currentShift.shift;
    const oldStaffId = currentShift.staffId; // Lưu vết nhân viên cũ

    // 4. Validate nhân viên mới nếu có thay đổi
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

    if (shift) {
        newShift = shift;
    }

    // 5. Validate trùng lặp ca, nghỉ phép, giới hạn ca
    if (staffId || shift) {
        const duplicate = await StaffShift.findOne({
            _id: { $ne: shiftId },
            staffId: newStaffId,
            workDate: fixedWorkDate,
            shift: newShift
        });
        if (duplicate) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has already been assigned this shift for the day.");
        }

        const approvedLeave = await LeaveRequest.findOne({
            staffId: newStaffId,
            workDate: fixedWorkDate,
            shift: newShift,
            status: "Approved"
        });
        if (approvedLeave) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has been approved for leave on this specific shift.");
        }

        const currentShiftsCount = await StaffShift.countDocuments({
            _id: { $ne: shiftId },
            staffId: newStaffId,
            workDate: fixedWorkDate
        });
        if (currentShiftsCount >= 2) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "This staff worked a maximum of two shifts on this day.");
        }
    }

    // 6. Cập nhật dữ liệu
    const isStaffChanged = staffId && newStaffId.toString() !== oldStaffId.toString();
    if (isStaffChanged) {
        currentShift.originalStaffId ??= currentShift.staffId;
        currentShift.staffId = newStaffId;
    }

    currentShift.shift = newShift;

    if (status) {
        currentShift.status = status;
    }

    await currentShift.save();
    
    // 7. Gửi thông báo khi Manager update ca làm
    const workDateStr = new Date(fixedWorkDate).toLocaleDateString('vi-VN');
    
    if (isStaffChanged) {
        Staff.findById(newStaffId).then(newStaff => {
            if (newStaff?.accountId) {
                NotificationService.createNotification({
                    accountId: newStaff.accountId,
                    title: "New shift assigned!",
                    message: `You have been assigned to a shift on ${workDateStr} (Shift ${newShift}).`,
                    type: "SHIFT_ASSIGNED",
                    metadata: { shiftId: currentShift._id.toString() }
                }).catch(err => console.error("Error notifying new staff:", err));
            }
        });
    } else {
        Staff.findById(oldStaffId).then(staff => {
            if (staff?.accountId) {
                NotificationService.createNotification({
                    accountId: staff.accountId,
                    title: "Shift schedule updated!",
                    message: `Your shift schedule on ${workDateStr} has been updated by the manager.`,
                    type: "SHIFT_UPDATED",
                    metadata: { shiftId: currentShift._id.toString() }
                }).catch(err => console.error("Error notifying updated staff:", err));
            }
        });
    }

    // 8. Trả về kết quả
    const populatedShift = await StaffShift.findById(currentShift._id)
        .populate({
            path: 'staffId',
            select: 'fullName position phone'
        })
        .populate({
            path: 'originalStaffId',
            select: 'fullName'
        })
        .lean();

    return {
        shiftId: populatedShift._id,
        staffId: populatedShift.staffId?._id,
        staffName: populatedShift.staffId?.fullName || "",
        position: populatedShift.staffId?.position || "",
        phone: populatedShift.staffId?.phone || "",
        workDate: populatedShift.workDate.toISOString().split('T')[0],
        shift: populatedShift.shift,
        status: populatedShift.status,
        originalStaffId: populatedShift.originalStaffId?._id || null,
        originalFullName: populatedShift.originalStaffId?.fullName || ""
    };
};


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
const getStaffForManager = async (accountId) => {
    const manager = await checkManager(accountId);
    
    const staffList = await Staff.find({ cinemaId: manager.cinemaId })
        .populate({
            path: 'accountId',
            select: 'status', 
            model: 'Account'
        })
        .lean();

    const activeStaffList = staffList
        .filter(s => s.accountId && s.accountId.status === 1) 
        .map(s => ({
            id: s._id,
            fullName: s.fullName || '',
            position: s.position || 'Staff',
            phone: s.phone || '',
            cinemaId: s.cinemaId,
            status: s.accountId.status 
        }));
    
    return {
        cinemaId: manager.cinemaId,
        staff: activeStaffList
    };
};

const generateShift = async (accountId, dateRange, config = {}, shiftsFromFE = []) => {
    // Vì thuật toán đã chạy ở FE, service ở đây đóng vai trò Validate & Insert dữ liệu thô nhận được từ FE
    if (!shiftsFromFE || !Array.isArray(shiftsFromFE) || shiftsFromFE.length === 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "No shift data provided for insertion.");
    }

    // B1: Chuẩn hóa định dạng dữ liệu & Ép kiểu từ chuỗi String sang Date object trong DB
    const formattedShifts = shiftsFromFE.map(item => ({
        staffId: item.staffId,
        cinemaId: item.cinemaId,
        workDate: new Date(item.workDate), // Ép kiểu string "yyyy-MM-dd" về Date UTC 00:00:00
        shift: item.shift,
        status: item.status || 'Assigned',
        isHoliday: item.isHoliday || false
    }));

    // B2: Thực hiện insert hàng loạt (Bulk Insert) xuống MongoDB với hiệu năng tối ưu
    const docs = await StaffShift.insertMany(formattedShifts);

    return {
        generated: docs.length,
        startDate: dateRange?.startDate,
        endDate: dateRange?.endDate,
        message: `Successfully allocated and saved ${docs.length} shifts into database.`
    };
};
const getWeekRange = (inputDate) => {
    let date = new Date(inputDate);

    if (!inputDate || isNaN(date.getTime())) {
        date = new Date();
    }

    // Clone date object to prevent side-effects
    const utcDate = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));

    const dayOfWeek = utcDate.getUTCDay();
    const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;

    const monday = new Date(utcDate);
    monday.setUTCDate(utcDate.getUTCDate() + diffToMonday);
    monday.setUTCHours(0, 0, 0, 0); // 00:00:00 UTC

    const sunday = new Date(monday);
    sunday.setUTCDate(monday.getUTCDate() + 6);
    sunday.setUTCHours(23, 59, 59, 999); // 23:59:59 UTC

    return { monday, sunday };
};
const formatDateString = (dateInput) => {
    if (!dateInput) return '';
    const d = new Date(dateInput);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
};


const getStaffDetailService = async (accountId, shiftId) => {
    // 1. Check manager permission
    const manager = await checkManager(accountId);
    const targetShift = await StaffShift.findById(shiftId)
        .populate({
            path: 'staffId',
            populate: { path: 'accountId', select: 'status' }
        })
        .lean();

    if (!targetShift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift information not found.");
    }

    checkCinemaPermission(manager.cinemaId, targetShift.cinemaId);

    const staff = targetShift.staffId;
    if (!staff) {
        throw new ApiError(StatusCodes.NOT_FOUND, "There is no staff member assigned to this shift yet.");
    }

    // 2. Lấy danh sách ca làm trong tuần
    const shiftDate = new Date(targetShift.workDate);
    const { monday, sunday } = getWeekRange(shiftDate);

    const shiftsInWeek = await StaffShift.find({
        staffId: staff._id,
        workDate: { $gte: monday, $lte: sunday }
    }).lean();

    // Sắp xếp ca làm theo thứ tự thời gian chuẩn
    const shiftOrder = { Morning: 1, Afternoon: 2, Evening: 3, Night: 4 };
    shiftsInWeek.sort((a, b) => {
        const timeA = new Date(a.workDate).getTime();
        const timeB = new Date(b.workDate).getTime();
        if (timeA !== timeB) return timeA - timeB;
        return (shiftOrder[a.shift] || 99) - (shiftOrder[b.shift] || 99);
    });

    // Helper format ngày ra dạng: "14 Jul"
    const formatDisplayDate = (date) => {
        const d = new Date(date);
        const day = d.getUTCDate();
        const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const month = monthNames[d.getUTCMonth()];
        return `${day} ${month}`;
    };

    // 3. Map dữ liệu lịch 7 ngày
    const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const weekSchedule = daysOfWeek.map((dayName, index) => {
        const currentDate = new Date(monday);
        currentDate.setUTCDate(monday.getUTCDate() + index);

        // Chuẩn hóa YYYY-MM-DD để so sánh chính xác ngày
        const isoDateStr = currentDate.toISOString().split('T')[0];

        const matchedShifts = shiftsInWeek.filter(
            s => new Date(s.workDate).toISOString().split('T')[0] === isoDateStr
        );

        // Đảm bảo các ca trùng trong ngày luôn được sắp xếp lại từ Sáng -> Đêm trước khi join
        matchedShifts.sort((a, b) => (shiftOrder[a.shift] || 99) - (shiftOrder[b.shift] || 99));

        const shiftNames = matchedShifts.map(s => s.shift);

        return {
            day: dayName,
            date: formatDisplayDate(currentDate), // Trả về dạng: "14 Jul"
            shift: shiftNames.length > 0 ? shiftNames.join(" - ") : null
        };
    });

    // 4. Tính Attendance Rate
    const [completedCount, absentCount] = await Promise.all([
        StaffShift.countDocuments({ staffId: staff._id, status: 'Completed' }),
        StaffShift.countDocuments({ staffId: staff._id, status: 'Absent' })
    ]);

    const totalEvaluated = completedCount + absentCount;
    const attendanceRate = totalEvaluated > 0
        ? Number(((completedCount / totalEvaluated) * 100).toFixed(1))
        : 100.0;

    return {
        shiftId: targetShift._id,
        workDate: formatDisplayDate(targetShift.workDate),
        shift: targetShift.shift,
        shiftStatus: targetShift.status,

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

        weekSchedule
    };
};const getLeaveRequestForManager = async (accountId, { status }) => {
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
            select: "fullName phone position"
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
        createdAt: item.createdAt,
        isShiftTaken: item.staffShiftId?.status !== "NeedReplacement"
    }));

}

const approveLeaveRequest = async (accountId, leaveRequestId) => {
    const manager = await checkManager(accountId);

    const leaveRequest = await LeaveRequest.findById(leaveRequestId).populate("staffShiftId").populate("staffId");
    if (!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found");
    }
    const shift = leaveRequest.staffShiftId;
    if (!shift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }
    
    // Check cinema permission
    checkCinemaPermission(manager.cinemaId, shift.cinemaId);
    
    // Check LeaveRequest status (only approve pending request)
    if (leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, `Cannot approve a request with status '${leaveRequest.status}'!`);
    }

    // Check staffShift status
    if (shift.status !== "Assigned") {
        throw new ApiError(StatusCodes.CONFLICT, `Shift is already in '${shift.status}' status.`);
    }

    // Transaction
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
        await LeaveRequest.updateOne(
            { _id: leaveRequestId },
            { 
                $set: { 
                    status: "Approved",
                    managerId: manager._id,
                    approvedAt: new Date()
                } 
            },
            { session }
        );

        await StaffShift.updateOne(
            { _id: shift._id },
            { $set: { status: "NeedReplacement" } },
            { session }
        );

        // Commit Transaction
        await session.commitTransaction();

        const staffAccount = leaveRequest.staffId?.accountId;
        const targetAccountId = staffAccount?._id || staffAccount;
        console.log("--> Debug Push Notification Staff Account ID:", targetAccountId);  

        // Gửi thông báo chuẩn hóa dùng `message`
        if (targetAccountId) {
            const workDateStr = leaveRequest.staffShiftId?.workDate 
                ? new Date(leaveRequest.staffShiftId.workDate).toLocaleDateString('vi-VN')
                : '';

            await NotificationService.createNotification({
                accountId: targetAccountId.toString(),
                title: "Leave Request Approved!",
                message: `Your leave request for shift on ${workDateStr} has been approved.`,
                type: "LEAVE_REQUEST_APPROVED",
                metadata: {
                    leaveRequestId: leaveRequest._id.toString(),
                    shiftId: leaveRequest.staffShiftId?._id?.toString() || ""
                }
            });
            console.log("--> Notification sent successfully to staff!");
        }
        return { success: true };

    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};


//reject leaveRequest - manager
const rejectLeaveRequest = async (accountId, leaveRequestId, { rejectReason }) => {
    const manager = await checkManager(accountId);
    const leaveRequest = await LeaveRequest.findById(leaveRequestId).populate("staffShiftId").populate("staffId");
    if (!leaveRequest) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found!");
    }

    const shift = leaveRequest.staffShiftId;
    if (!shift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Associated shift not found!");
    }
    
    // Check permission
    checkCinemaPermission(manager.cinemaId, shift.cinemaId);
    
    // Check status
    if (leaveRequest.status !== "Pending") {
        throw new ApiError(StatusCodes.CONFLICT, `This leave request has already been ${leaveRequest.status.toLowerCase()}!`);
    }
    
    const formattedReason = rejectReason && rejectReason.trim() !== "" ? rejectReason : "No specific reason provided.";

    leaveRequest.status = "Rejected";
    leaveRequest.managerId = manager._id;
    leaveRequest.rejectReason = formattedReason;
    leaveRequest.approvedAt = new Date();
    await leaveRequest.save();

    // Gửi thông báo chuẩn hóa dùng `message`
    if (leaveRequest.staffId?.accountId) {
        NotificationService.createNotification({
            accountId: leaveRequest.staffId.accountId,
            title: "Shift leave request denied!",
            message: `Your request for time off for the shift on ${new Date(shift.workDate).toLocaleDateString('vi-VN')} has been rejected. Reason: ${formattedReason}`,
            type: "LEAVE_REQUEST_REJECTED",
            metadata: { leaveRequestId: leaveRequest._id.toString() }
        }).catch(err => console.error("Error sending rejection notification:", err));
    }

    return { success: true };
};

const undoLeaveRequest = async (accountId, leaveRequestId) => {
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
        const manager = await checkManager(accountId);
        
        // Populate thêm staffId để lấy accountId gửi notification
        const leaveRequest = await LeaveRequest.findById(leaveRequestId)
            .populate("staffShiftId")
            .populate("staffId")
            .session(session);

        if (!leaveRequest) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Leave request not found!");
        }

        const shift = leaveRequest.staffShiftId;
        if (!shift) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Associated shift not found!");
        }

        // Check permission
        checkCinemaPermission(manager.cinemaId, shift.cinemaId);

        // Check status
        if (leaveRequest.status === "Pending") {
            throw new ApiError(StatusCodes.CONFLICT, "This leave request is already pending!");
        }

        if (leaveRequest.status === 'Approved') {
            if (shift.status !== 'NeedReplacement') {
                throw new ApiError(StatusCodes.CONFLICT, "Cannot undo because another staff has already accepted this shift or shift is processed!");
            }

            shift.status = 'Assigned';
            await shift.save({ session });
        } 
   
        leaveRequest.status = 'Pending';
        leaveRequest.managerId = null;
        leaveRequest.approvedAt = null;
        leaveRequest.rejectReason = null; 

        await leaveRequest.save({ session });
        
        await session.commitTransaction();
        session.endSession();

        // GỬI THÔNG BÁO CHO NHÂN VIÊN (Gọi sau khi commit transaction)
        if (leaveRequest.staffId?.accountId) {
            const workDateStr = new Date(shift.workDate).toLocaleDateString('vi-VN');
            
            NotificationService.createNotification({
                accountId: leaveRequest.staffId.accountId,
                title: "Leave request status updated!",
                message: `Your leave request for the shift on ${workDateStr} has been reset to Pending status by the manager.`,
                type: "LEAVE_REQUEST_UPDATED",
                metadata: { leaveRequestId: leaveRequest._id.toString() }
            }).catch(err => console.error("Error sending undo leave request notification:", err));
        }

        return leaveRequest;

    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};

export const ManagerStaffShiftService = {
    updateShifts,
    generateShift,
    getShifts,
    getStaffForManager,
    getStaffDetailService,
    getLeaveRequestForManager,
    approveLeaveRequest,
    rejectLeaveRequest,
    undoLeaveRequest
};
