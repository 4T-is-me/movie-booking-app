import { StatusCodes } from 'http-status-codes';
import  { managerDashboardService} from '../services/managerDashboardService.js';
import ApiError from "../utils/ApiError.js";ApiError
const getManagerReport = async (req, res, next) => {
    try {
        const {type = 'day'} = req.query;
        const validateType = ['day', 'week', 'month', 'year'];
        if(!validateType.includes(type)) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "Loại báo cáo (type) không hợp lệ! Chỉ chấp nhận: day, week, month, year");
        }

        const accountId = req.user.accountId
        
        const reportData = await managerDashboardService.getManagerReport(accountId, type);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: `Lấy báo cáo theo ${type} thành công!`,
            data: reportData
        });

    } catch (error) {
        next(error);
    }
}

export const managerReportController = { 
    getManagerReport 
};