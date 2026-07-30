import { Payroll } from '../models/payrollModel.js';
import { Staff } from '../models/staffModel.js';
import { Manager } from '../models/managerModel.js';
import { StaffShift } from "../models/staffShiftModel.js";
import { LeaveRequest } from "../models/leaveRequestModel.js";
import { Showtime } from "../models/showTimeModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from '../utils/Constants.js';
import { payrollConfigService } from './payrollConfigService.js';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';
/**
 * Tính lương cho 1 Staff
 */
const calculateStaffPayroll = async (staff, month, year) => {
  const startDate = new Date(Date.UTC(year, month - 1, 1, 0, 0, 0, 0));
  const endDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));

  // 1. Lấy tất cả ca làm của nhân viên trong tháng
  const shifts = await StaffShift.find({
    staffId: staff._id,
    workDate: { $gte: startDate, $lte: endDate },
  }).lean();

  if (shifts.length === 0) {
    return {
      employeeId: staff._id,
      cinemaId: staff.cinemaId,
      month, year,
      baseSalary: 0, shiftSalary: 0, bonus: 0, penalty: 0, totalSalary: 0,
      status: 'Draft',
      generatedAt: new Date(),
      shiftIds: []
    };
  }

  // 2. Lấy tất cả LeaveRequest đã Approved liên quan đến các shift này TRONG 1 QUERY
  const shiftIds = shifts.map(s => s._id);
  const approvedLeaves = await LeaveRequest.find({
    staffShiftId: { $in: shiftIds },
    status: 'Approved'
  }).select('staffShiftId').lean();

  // Đưa vào Set để lookup với độ phức tạp O(1)
  const approvedShiftIdsSet = new Set(approvedLeaves.map(l => l.staffShiftId.toString()));

  let shiftSalary = 0;
  let penalty = 0;
  const bonus = 0;

  // 3. Tính toán tiền
  for (const shift of shifts) {
    // Nếu ca này không phải ca làm thay (!originalStaffId) VÀ có đơn nghỉ phép được duyệt -> Bỏ qua
    if (!shift.originalStaffId && approvedShiftIdsSet.has(shift._id.toString())) {
      continue;
    }

    const baseRate = Number(shift.shiftRate || 0);

    if (shift.status === 'Completed') {
      shiftSalary += Number(shift.salary || 0);
    } else if (shift.status === 'Absent') {
      penalty += baseRate;
    }
  }

  const totalSalary = Math.max(0, shiftSalary + bonus - penalty);

  return {
    employeeId: staff._id,
    cinemaId: staff.cinemaId,
    month,
    year,
    baseSalary: 0,
    shiftSalary,
    bonus,
    penalty,
    totalSalary,
    status: 'Draft',
    generatedAt: new Date(),
    shiftIds,
  };
};

/**
 * Tính lương cho 1 Manager
 */
const calculateManagerPayroll = async (manager, month, year) => {
  const startDate = new Date(Date.UTC(year, month - 1, 1, 0, 0, 0, 0));
  const endDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));

  const baseSalary = manager.salary || 0;
  let bonus = 0;
  const payrollConfig = await payrollConfigService.getConfig();

  // Lấy danh sách Suất chiếu thuộc Cinema do Manager quản lý
  const showtimes = await Showtime.find({ cinemaId: manager.cinemaId }).select('_id');
  const showtimeIds = showtimes.map((s) => s._id);

  // Tính tổng doanh thu thành công của Rạp trong tháng
  const revenueResult = await Booking.aggregate([
    {
      $match: {
        showtimeId: { $in: showtimeIds },
        paymentStatus: 'paid',
        createdAt: { $gte: startDate, $lte: endDate },
      },
    },
    {
      $group: {
        _id: null,
        totalRevenue: { $sum: '$finalAmount' },
      },
    },
  ]);

  const actualRevenue = revenueResult[0]?.totalRevenue || 0;

  // Nếu đạt Target doanh thu -> Thưởng
  if (actualRevenue >= manager.revenueTarget) {
    bonus += payrollConfig.managerRevenueBonus ?? constants.MANAGER_REVENUE_BONUS;
  }

  const totalSalary = baseSalary + bonus;

  return {
    employeeId: manager._id,
    cinemaId: manager.cinemaId,
    month,
    year,
    baseSalary,
    shiftSalary: 0,
    bonus,
    penalty: 0,
    totalSalary,
    status: 'Draft',
    generatedAt: new Date(),
  };
};

const validatePayrollPeriod = (month, year) => {
  const now = new Date();
  const currentYear = now.getUTCFullYear();
  const currentMonth = now.getUTCMonth() + 1; // getUTCMonth() trả từ 0 - 11

  // Chặn nếu năm lớn hơn hoặc (cùng năm nhưng tháng lớn hơn)
  if (year > currentYear || (year === currentYear && month > currentMonth)) {
    throw new ApiError(
      StatusCodes.BAD_REQUEST, 
      `Cannot generate payroll for future periods (${month}/${year}). Current period is ${currentMonth}/${currentYear}.`
    );
  }
};

/**
 * Hàm này dùng nội bộ để gom mảng employeeIds lại và truy vấn 1 lần.
 */
const getBulkEmployeeInfo = async (employeeIds) => {
  // Lọc ID trùng lặp để giảm thiểu data query
  const uniqueIds = [...new Set(employeeIds.map(id => id.toString()))];
  if (uniqueIds.length === 0) return {};

  // Tìm trong bảng Staff
  const staffs = await Staff.find({ _id: { $in: uniqueIds } })
    .populate({
      path: "accountId",
      select: "username roleId",
      populate: { path: "roleId", select: "roleName" }
    }).lean();

  const foundStaffIds = staffs.map(s => s._id.toString());
  const missingIds = uniqueIds.filter(id => !foundStaffIds.includes(id));

  // Nếu có ID chưa tìm thấy (là Manager), tìm trong bảng Manager
  let managers = [];
  if (missingIds.length > 0) {
    managers = await Manager.find({ _id: { $in: missingIds } })
      .populate({
        path: "accountId",
        select: "username roleId",
        populate: { path: "roleId", select: "roleName" }
      }).lean();
  }

  // Gom chung lại và tạo Dictionary (Map) để map dữ liệu với độ phức tạp O(1)
  const allEmployees = [...staffs, ...managers];
  const employeeMap = {};

  for (const emp of allEmployees) {
    employeeMap[emp._id.toString()] = {
      _id: emp._id,
      fullName: emp.fullName,
      username: emp.accountId?.username || null,
      image: emp.image || null,
      role: emp.accountId?.roleId?.roleName || null,
    };
  }

  return employeeMap;
};

/**
 * Generate Payroll hàng loạt cho toàn bộ Staff & Manager
 */
const generatePayroll = async (month, year) => {
  // CHẶN TẠO LƯƠNG TƯƠNG LAI Ở ĐÂY
  validatePayrollPeriod(month, year);

  const payrollsToInsert = [];

  // Lấy danh sách những người ĐÃ ĐƯỢC TÍNH LƯƠNG trong tháng/năm này rồi
  const existingPayrolls = await Payroll.find({ month, year }).select('employeeId').lean();
  const existingEmployeeIds = new Set(existingPayrolls.map(p => p.employeeId.toString()));

  // 1. Xử lý tất cả Staff
  const staffList = await Staff.find().lean();
  for (const staff of staffList) {
    if (!existingEmployeeIds.has(staff._id.toString())) {
      const payrollData = await calculateStaffPayroll(staff, month, year);
      payrollsToInsert.push(payrollData);
    }
  }

  // 2. Xử lý tất cả Manager
  const managerList = await Manager.find().lean();
  for (const manager of managerList) {
    if (!existingEmployeeIds.has(manager._id.toString())) {
      const payrollData = await calculateManagerPayroll(manager, month, year);
      payrollsToInsert.push(payrollData);
    }
  }

  // 3. Bulk insert
  if (payrollsToInsert.length > 0) {
    return await Payroll.insertMany(payrollsToInsert);
  }

  return [];
};

/*
 * Hàm đếm ca làm thực tế của Staff trong tháng (dùng cho API xem chi tiết)
*/
const getStaffShiftCount = async (staffId, month, year) => {
  const startDate = new Date(Date.UTC(year, month - 1, 1, 0, 0, 0, 0));
  const endDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));

  // Thêm .lean() để tăng tốc độ read
  const shifts = await StaffShift.find({
    staffId: staffId,
    workDate: { $gte: startDate, $lte: endDate },
  }).lean();

  // Gom tất cả shift_id cần check leave request
  const potentialLeaveShiftIds = shifts
    .filter(shift => !shift.originalStaffId)
    .map(shift => shift._id);

  let approvedShiftIdsSet = new Set();
  
  // Chỉ query LeaveRequest 1 lần duy nhất bằng $in
  if (potentialLeaveShiftIds.length > 0) {
    const approvedLeaves = await LeaveRequest.find({
      staffShiftId: { $in: potentialLeaveShiftIds },
      status: 'Approved',
    }).select('staffShiftId').lean();

    approvedShiftIdsSet = new Set(approvedLeaves.map(l => l.staffShiftId.toString()));
  }

  const shiftCount = {
    morning: 0,
    afternoon: 0,
    evening: 0,
    night: 0,
    absent: 0,
  };

  // Vòng lặp tính toán lúc này chỉ chạy trên RAM, không chọc xuống DB nữa
  for (const shift of shifts) {
    if (!shift.originalStaffId && approvedShiftIdsSet.has(shift._id.toString())) {
      continue; 
    }

    if (shift.status === 'Completed') {
      const shiftName = shift.shift?.toLowerCase();
      if (shiftCount.hasOwnProperty(shiftName)) {
        shiftCount[shiftName]++;
      }
    } else if (shift.status === 'Absent') {
      shiftCount.absent++;
    }
  }

  return shiftCount;
};

const getManagerPayrolls = async (accountId, filter = {}) => {
  const manager = await Manager.findOne({ accountId }).lean(); // Thêm lean() 

  if (!manager) {
    throw new Error("Manager not found");
  }

  const query = {
    cinemaId: manager.cinemaId,
    ...filter
  };

  const payrolls = await Payroll.find(query)
    .populate("cinemaId", "_id cinemaName")
    .lean();

  // Gom tất cả employeeId lại và query 1 lần
  const employeeIds = payrolls.map(p => p.employeeId);
  const employeeInfoMap = await getBulkEmployeeInfo(employeeIds);

  // Map lại vào kết quả (Thay vì dùng Promise.all và gọi DB từng lần)
  return payrolls.map(payroll => ({
    ...payroll,
    employeeId: employeeInfoMap[payroll.employeeId.toString()] || null
  }));
};

const getMyPayroll = async (accountId) => {
  let employee = await Staff.findOne({ accountId }).select("_id").lean();

  if (!employee) {
    employee = await Manager.findOne({ accountId }).select("_id").lean();
  }

  if (!employee) {
    throw new Error("Employee not found");
  }

  const payrolls = await Payroll.find({
    employeeId: employee._id
  })
    .populate("cinemaId", "_id cinemaName")
    .sort({ year: -1, month: -1 })
    .lean();

  const employeeIds = payrolls.map(p => p.employeeId);
  const employeeInfoMap = await getBulkEmployeeInfo(employeeIds);

  return payrolls.map(payroll => ({
    ...payroll,
    employeeId: employeeInfoMap[payroll.employeeId.toString()] || null
  }));
};
const getEmployeeInfo = async (employeeId) => { 
  let employee = await Staff.findById(employeeId)
  .populate({ 
    path: "accountId", 
    select: "username roleId", 
    populate: { path: "roleId", select: "roleName" } 
  }); 
  if (!employee) { 
    employee = await Manager.findById(employeeId)
    .populate({ 
      path: "accountId", 
      select: "username roleId", 
      populate: { path: "roleId", select: "roleName" } 
    }); 
  } 
  if (!employee) { 
    return null; 
  } 
  return { 
    _id: employee._id, 
    fullName: employee.fullName, 
    username: employee.accountId?.username || null, 
    image: employee.image || null, 
    role: employee.accountId?.roleId?.roleName || null, 
  } 
};

export const payrollService = {
  generatePayroll,
  calculateStaffPayroll,
  calculateManagerPayroll,
  getEmployeeInfo,
  getStaffShiftCount,
  getManagerPayrolls,
  getMyPayroll
};
