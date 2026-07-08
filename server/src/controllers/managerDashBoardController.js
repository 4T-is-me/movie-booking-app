import { Booking } from '../models/bookingModel.js';
import { StatusCodes } from 'http-status-codes';
import  { managerDashboardService} from '../services/managerDashboardService.js';
const getManagerDashboard = async (req, res, next) => {
    try {
     const accountId = req.user.accountId
        const dashboardData = await managerDashboardService.getManagerDashboard(accountId);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: 'Tải dữ liệu Dashboard hôm nay thành công!',
            data: dashboardData
        });
            
    } catch (error) {
        next(error)
    }            
}

export const managerDashboardController = {
    getManagerDashboard
}

