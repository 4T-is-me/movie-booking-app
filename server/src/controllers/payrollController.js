import { StatusCodes } from "http-status-codes";
import { payrollService } from "../services/payrollService.js";
import { Payroll } from "../models/payrollModel.js";

const generatePayroll = async (req, res, next) => {
  try {
    const { month, year } = req.body;

    if (!month || !year || month < 1 || month > 12) {
      return res.status(StatusCodes.BAD_REQUEST).json({ message: 'Tháng và năm không hợp lệ!' });
    }

    const result = await payrollService.generatePayroll(Number(month), Number(year));

    return res.status(StatusCodes.OK).json({
      message: `Successfully created ${result.length} payroll records.`,
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

const getAllPayrolls = async (req, res, next) => {
  try {
    const { month, year, cinemaId, status } = req.query;
    const filter = {};

    if (month) filter.month = Number(month);
    if (year) filter.year = Number(year);
    if (cinemaId) filter.cinemaId = cinemaId;
    if (status) filter.status = status;

    // 1. Query dữ liệu và populate cinemaId (chỉ lấy _id và cinemaName)
    const payrolls = await Payroll.find(filter)
      .populate('cinemaId', '_id cinemaName')
      .lean()
      .sort({ createdAt: -1 });

    // 2. Lấy thông tin chi tiết nhân viên cho từng payroll
    const updatedPayrolls = await Promise.all(
      payrolls.map(async (payroll) => {
        const employeeInfo = await payrollService.getEmployeeInfo(payroll.employeeId);
        return {
          ...payroll,
          employeeId: employeeInfo // Thay thế ObjectId String bằng Object { _id, fullName, role,... }
        };
      })
    );

    return res.status(StatusCodes.OK).json({
      success: true,
      count: updatedPayrolls.length,
      data: updatedPayrolls,
    });
  } catch (error) {
    next(error);
  }
};

const getPayrollById = async (req, res, next) => {
  try {
    const payroll = await Payroll.findById(req.params.id)
      .populate('cinemaId', '_id cinemaName')
      .lean();

    if (!payroll) {
      return res.status(StatusCodes.NOT_FOUND).json({ message: 'Payroll not found!' });
    }

    //  Lấy thông tin nhân viên
    const employeeInfo = await payrollService.getEmployeeInfo(payroll.employeeId);

    //  Tính shiftCount động khi chạy vào hàm detail (chỉ áp dụng cho Staff)
    let shiftCount = {
      morning: 0,
      afternoon: 0,
      evening: 0,
      night: 0,
      absent: 0
    };

    // Nếu là Staff thì mới đếm ca (Manager giữ nguyên 0)
    if (employeeInfo && employeeInfo.role?.toLowerCase().includes('staff')) {
      shiftCount = await payrollService.getStaffShiftCount(
        payroll.employeeId,
        payroll.month,
        payroll.year
      );
    }

    //  Trả về response kèm trường shiftCount
    const responseData = {
      ...payroll,
      employeeId: employeeInfo || null,
      shiftCount: shiftCount
    };

    return res.status(StatusCodes.OK).json({
      success: true,
      data: responseData,
    });
  } catch (error) {
    next(error);
  }
};
const markAsPaid = async (req, res, next) => {
  try {
    const payroll = await Payroll.findById(req.params.id);

    if (!payroll) {
      return res.status(StatusCodes.NOT_FOUND).json({ message: 'Payroll not found!' });
    }

    if (payroll.status === 'Paid') {
      return res.status(StatusCodes.BAD_REQUEST).json({ message: 'This payroll has already been paid!' });
    }

    payroll.status = 'Paid';
    payroll.paidAt = new Date();
    await payroll.save();

    return res.status(StatusCodes.OK).json({
      message: 'Payment status updated to successful!',
      data: payroll,
    });
  } catch (error) {
    next(error);
  }
};

const getManagerPayrolls = async (req, res, next) => {
    try {

        const accountId = req.user.accountId;
        const payrolls =
            await payrollService.getManagerPayrolls(
                accountId,
                req.query
            );

        return res.status(StatusCodes.OK).json({
            success: true,
            count: payrolls.length,
            data: payrolls
        });

    } catch (err) {
        next(err);
    }
};

const getMyPayroll = async (req, res, next) => {

    try {

        const accountId = req.user.accountId;

        const payrolls =
            await payrollService.getMyPayroll(accountId);

        return res.status(StatusCodes.OK).json({
            success: true,
            count: payrolls.length,
            data: payrolls
        });

    } catch (err) {
        next(err);
    }

};

export const payrollController = {
  generatePayroll,
  getAllPayrolls,
  getPayrollById,
  markAsPaid,
  getManagerPayrolls,
  getMyPayroll
};