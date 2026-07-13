import { Manager } from "../models/managerModel.js";
import { Staff } from "../models/staffModel.js";
import { StaffShift } from "../models/staffShiftModel.js";
import { LeaveRequest } from "../models/leaveRequestModel.js";
import { StatusCodes } from "http-status-codes";
import { constants} from "../utils/Constants.js";
import ApiError from "../utils/ApiError.js";
import mongoose from "mongoose";


const getShifts = async (accountId, query) => {
    const { staffId, workDate, startDate, endDate, shift } = query;

    const manager = await Manager.findOne({ accountId})
    if(!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
    }
    const filter = {
        cinemaId: manager.cinemaId,
    }
    if(staffId) {
        filter.staffId = staffId;
    }
    if(shift) {
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

    shifts.sort((a, b) => {
        if (a.workDate.getTime() !== b.workDate.getTime()) {
            return a.workDate - b.workDate;
        }

        return shiftOrder[a.shift] - shiftOrder[b.shift];
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


// unfinished
const createShift = async (accountId, shiftData) => {
    const {staffId, workDate, shift} = shiftData;

    //check manager
    const manager = await Manager.findOne({ accountId });
    if (!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
        
    }

    //get managerCinemaId
    const managerCinemaId = manager.cinemaId;

    //check staff
    const staff = await Staff.findById(staffId);
    if (!staff) {
        throw new ApiError(StatusCodes.NOT_FOUND, "The staff does not exist.");
    }

    //check staff is the same cinema with manager
    if (staff.cinemaId.toString() !== managerCinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only schedule shifts for staff members belonging to your own cinema.");
    }

    //(Range Query 00:00:00 - 23:59:59)
    const startOfDay = new Date(workDate);
    startOfDay.setUTCHours(0, 0, 0, 0);

    const endOfDay = new Date(workDate);
    endOfDay.setUTCHours(23, 59, 59, 999);

    const dateQuery = { $gte: startOfDay, $lte: endOfDay };

    // Chặn tạo ca cho ngày trong quá khứ
    const todayStart = new Date();

    todayStart.setUTCHours(0, 0, 0, 0);
    if (startOfDay < todayStart) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Cannot create a shift for a past date.");
    }

    //check duplicate
    const isDuplicate = await StaffShift.findOne({ staffId, workDate: dateQuery, shift });
    if (isDuplicate) {
        throw new ApiError(StatusCodes.CONFLICT, "This staff has already been assigned this shift for the day.");
    }

    // check the shift scheduling date falls within the range from FromDate to ToDate of an Approved order
    const hasApprovedLeave = await LeaveRequest.findOne({
        staffId,
        status: "Approved",
        fromDate: { $lte: endOfDay },
        toDate: { $gte: startOfDay },
    })

    if(hasApprovedLeave) {
        throw new ApiError(StatusCodes.CONFLICT, "Unable to schedule. The staff has already been approved for leave on this day.");
    }

    //check maximum shift/day
    const currentStaffShiftCount = await StaffShift.countDocuments({ staffId, workDate: dateQuery });
    if (currentStaffShiftCount >= constants.MAX_SHIFT_PER_DAY) {
        throw new ApiError(StatusCodes.CONFLICT, `This staff has reached the maximum limit of ${constants.MAX_SHIFT_PER_DAY} shifts per day.`);
    }

    //check xem Ca đó trong ngày đã đủ số lượng người chưa
    const currentShiftPeopleCount = await StaffShift.countDocuments({ 
        cinemaId: managerCinemaId, 
        workDate: dateQuery, 
        shift 
    });

    if (currentShiftPeopleCount >= constants.STAFF_PER_SHIFT) {
        throw new ApiError(StatusCodes.CONFLICT, `The ${shift} shift on this day already has the required ${constants.STAFF_PER_SHIFT} staff members.`);
    }

    const newShift =  await StaffShift.create({ staffId, cinemaId: managerCinemaId, workDate: startOfDay, shift, status: 'Assigned' });
    
    //join the shift to staff
    const populatedShift = await StaffShift.findById(newShift._id).populate({ path: 'staffId', select: 'fullName position phone' 

    })
    return {
        shiftId: populatedShift._id,
        staffName: populatedShift.staffId?.fullName || "",
        position: populatedShift.staffId?.position || "",
        cinemaId: populatedShift.cinemaId,
        workDate: populatedShift.workDate,
        shift: populatedShift.shift,
        status: populatedShift.status
    }
}

//unfinished
const updateShifts = async (accountId, shiftId, updateData) => {
    const { staffId, workDate, shift, status } = updateData;
    
    const manager = await Manager.findOne({accountId})
    if(!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
    }
    
    const currentShift = await StaffShift.findById(shiftId);
    if(!currentShift) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Shift not found!");
    }

    if(currentShift.cinemaId.toString() !== manager.cinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only update shifts for staff members belonging to your own cinema.");
    }
    
    let newStaffId = currentShift.staffId
    let newWorkDate = currentShift.workDate
    let newShift = currentShift.shift

    if(staffId) {
        const staff = await Staff.findById(staffId);
        if(!staff) {
            throw new ApiError(StatusCodes.NOT_FOUND, "The staff does not exist.");
        }

        if(staff.cinemaId.toString() !== manager.cinemaId.toString()) {
            throw new ApiError(StatusCodes.FORBIDDEN, "You can only update shifts for staff members belonging to your own cinema.");
        }
        newStaffId = staff._id
    }
    //update workDate
    if(workDate) {
        newWorkDate = new Date(workDate);
        newWorkDate.setHours(0, 0, 0, 0);
    }
    //update shift
    if(shift) {
        newShift = shift;
    }

    //check duplicate, limit shift
    if (staffId || workDate || shift) {
        // Kiểm tra trùng lặp ca trùng
        const duplicate = await StaffShift.findOne({
            _id: { $ne: shiftId }, 
            staffId: newStaffId,
            workDate: newWorkDate,
            shift: newShift
        });
        if (duplicate) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has already been assigned this shift for the day.");
        }

        // Kiểm tra lịch nghỉ phép của nhân viên
        const approvedLeave = await LeaveRequest.findOne({
            staffId: newStaffId,
            fromDate: { $lte: newWorkDate },
            toDate: { $gte: newWorkDate },
            status: "Approved"
        });
        if (approvedLeave) {
            throw new ApiError(StatusCodes.CONFLICT, "This staff has been approved for leave on this day.");
        }

        // check shift limit 3/day
        const currentShiftsCount = await StaffShift.countDocuments({
            _id: { $ne: shiftId },
            staffId: newStaffId,
            workDate: newWorkDate
        });
        if (currentShiftsCount >= 2) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "This staff worked a maximum of two shifts on this day.");
        }
    }
    //update
    currentShift.staffId = newStaffId;
    currentShift.workDate = newWorkDate;
    currentShift.shift = newShift;

    if(status) {
        currentShift.status = status;
    }
    await currentShift.save();
    const populatedShift = await StaffShift.findById(currentShift._id)
    .populate({
       path: 'staffId',
       select: 'fullname position phone' 
    })
    .lean();

    return {
        shiftId: populatedShift._id,
        staffName: populatedShift.staffId?.staffName || "",
        position: populatedShift.staffId?.position || "",
        phone: populatedShift.staffId?.phone || "",
        workDate: populatedShift.workDate.toLocaleDateString('sv-SE'),
        shift: populatedShift.shift,
        status: populatedShift.status
    }

}

//aggregate current shifts for the day at the cinema
const getExistingShiftsOfDay = async (cinemaId, workDate) => {
    const exsitingShifts = await StaffShift.find({
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        workDate: workDate
    }).lean();
    
    const shiftOccupancy = {Morning: [], Afternoon: [], Evening: [], Night: []};
    const staffDailyCount = {}
    
    exsitingShifts.forEach(shift => {
        if(shiftOccupancy[shift.shift]) {
            shiftOccupancy[shift.shift].push(shift.staffId.toString());
        }
        const staffIdStr = shift.staffId.toString();
        staffDailyCount[staffIdStr] = (staffDailyCount[staffIdStr] ||0) + 1
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
    const absentStaffIds = approvedLeaves.filter(leave =>
        leave.staffId.cinemaId.toString() === cinemaId.toString()
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
    if(totalStaff === 0) {
        return null;
    }
    for(let i = 0; i < totalStaff; i++) {
        const currentIndex = (context.staffIndex + i) % totalStaff;
        const staff = dayStaffs[currentIndex];
        const staffIdStr = staff._id.toString();

        const alreadyInshift = shiftOccupancy[currentShift].includes(staffIdStr);
        const currentCount = staffDailyCount[staffIdStr] || 0;

        if (!alreadyInshift && currentCount < constants.MAX_SHIFT_PER_DAY) {
            context.staffIndex = (currentIndex +  1) % totalStaff;
            return staffIdStr;
        }
    }
    return null;
}

//main generate shift - unfinished
const generateShift = async (accountId, dateRange) => {
    const {startDate, endDate} = dateRange;

    //normalize date
    const currentDate = new Date(startDate)
    currentDate.setUTCHours(0, 0, 0, 0);
    const finalDate = new Date(endDate)
    finalDate.setUTCHours(0, 0, 0, 0); 

    if (currentDate > finalDate) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "The start date cannot be later than the end date.");
    }

    //check no generate in the past
    const today = new Date();
    today.setUTCHours(0, 0, 0, 0);
    if (currentDate < today) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "It is not possible to automatically schedule appointments for past dates.");
    }

    //check manager
    const manager = await Manager.findOne({ accountId });
    if (!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
    }
    const cinemaId = manager.cinemaId;

    //get all staff
    const allStaff = await Staff.find({ cinemaId }).lean();
    if (!allStaff || allStaff.length === 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "The cinema currently has no staff to schedule for shifts.");
    }
    //The context maintains the Round-Robin pointer state across days.
    const context = { staffIndex: 0 };
    const newShiftsToInsert = [];

    // Iterate through each day
    while (currentDate <= finalDate) {
        //   Mốc startOfDay chuẩn 00:00:00 UTC của ngày hiện tại
        const targetDate = new Date(currentDate);
        targetDate.setUTCHours(0, 0, 0, 0);

        //get staff for day(cinema leave requests deducted)
        const dayStaff = await getAvailableStaffForDay(allStaff, cinemaId, targetDate);

        //Check the total number of required slots against the maximum capacity for that day early on.
        const totalNeedInDay = constants.SHIFTS.length * constants.STAFF_PER_SHIFT; // 4 shift * 2 person = 8 slots
        const totalCanAssignInDay = dayStaff.length * constants.MAX_SHIFT_PER_DAY; // staff ON * 2 shift
        
        if (totalCanAssignInDay < totalNeedInDay) {
            const formattedDate = targetDate.toLocaleDateString('vi-VN');
            throw new ApiError(
                StatusCodes.BAD_REQUEST, 
                `On ${formattedDate}, only ${dayStaff.length} staff members are available (capable of handling a maximum of ${totalCanAssignInDay} shifts), which is insufficient to meet the system's minimum requirement of ${totalNeedInDay} shifts. Please add more staff first.`
            );
        }
        // Retrieve the old schedule for the day under consideration from the db
        const { shiftOccupancy, staffDailyCount } = await getExistingShiftsOfDay(cinemaId, targetDate);

        //Review each shift of the day
        for (const shiftName of constants.SHIFTS) {
            const currentPeopleCount = shiftOccupancy[shiftName].length;
            const neededPeopleCount = constants.STAFF_PER_SHIFT - currentPeopleCount;

            //Ca đã đủ người -> Dịch chuyển con trỏ công bằng để không bị đứng im một chỗ
            if (neededPeopleCount <= 0) {
                if (dayStaff.length > 0) {
                    context.staffIndex = (context.staffIndex + STAFF_PER_SHIFT) % dayStaff.length;
                }
                continue;
            }
            // Tiến hành bốc người cho các slot còn thiếu
            for (let k = 0; k < neededPeopleCount; k++) {
                const selectedStaffId = pickNextStaffRoundRobin( dayStaff, shiftName, shiftOccupancy, staffDailyCount, context );

                //Chặn khuyết ca
                if (!selectedStaffId) {
                    const formattedDate = targetDate.toLocaleDateString('vi-VN');
                    throw new ApiError( StatusCodes.BAD_REQUEST, 
                            `Allocation error: No suitable personnel found for shift [${shiftName}] on ${formattedDate} due to a shift overlap conflict.`
                    );
                }
                // Cập nhật RAM tạm thời của ngày
                shiftOccupancy[shiftName].push(selectedStaffId);
                staffDailyCount[selectedStaffId] = (staffDailyCount[selectedStaffId] || 0) + 1;

                //Lưu vào mảng chờ
                newShiftsToInsert.push({
                    staffId: selectedStaffId,
                    cinemaId: cinemaId,
                    workDate: targetDate,
                    shift: shiftName,
                    status: 'Assigned'
                });
            }
        }
        currentDate.setUTCDate(currentDate.getUTCDate() + 1);
    }
    //Lưu hàng loạt sau khi vượt qua tất cả kiểm tra nghiêm ngặt
    let generatedCount = 0;
    if (newShiftsToInsert.length > 0) {
        const docs = await StaffShift.insertMany(newShiftsToInsert);
        generatedCount = docs.length;
    }

    return {
        generated: generatedCount,
        startDate: startDate,
        endDate: endDate,
        message: `The system has successfully and automatically allocated ${generatedCount} shifts from ${startDate} to ${endDate}.`
    };
    
}

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
    const manager = await Manager.findOne({accountId})
    if(!manager) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function.");
    }
    //find the staff
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
    if (targetShift.cinemaId.toString() !== manager.cinemaId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You can only view shifts at your own cinema.");
    }

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


export const ManagerStaffShiftService = {
  createShift, 
  updateShifts,
  generateShift,
  getShifts,
  getStaffDetailService
};
