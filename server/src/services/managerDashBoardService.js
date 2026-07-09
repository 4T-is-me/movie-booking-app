import { Booking } from '../models/bookingModel.js';
import { CinemaRoom } from '../models/cinemaRoomModel.js';
import { Showtime } from '../models/showTimeModel.js';
import { Manager } from '../models/managerModel.js';
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from "http-status-codes";
import mongoose from "mongoose";

/**
 * get Information of Manager
 * @param {string} accountId 
 * @returns {Promise<Object>} manager document
 */
const getManagerById = async (accountId) => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) {
        throw new ApiError( StatusCodes.NOT_FOUND, 'Không tìm thấy thông tin Quản lý cho tài khoản này!');
    }
    return manager;
};


/**
 * Tính tổng doanh thu ngày hôm nay của một rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Tổng số tiền doanh thu (đã trừ giảm giá)
 */
const getRevenueToday = async (cinemaId) => {
    // start of day tính theo múi giờ +07
    const startOfDay = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    // end of day tính theo múi giờ +07
    const endOfDay = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);

    const result = await Booking.aggregate([
        //match time and payment status 
        {
            $match: {
                paymentStatus: 'paid',
                createdAt: {
                    $gte: startOfDay,
                    $lte: endOfDay
                }
            }
        },
        //lookup with records 
        {
            $lookup:{
                from: 'showtimes',
                localField: 'showtimeId',
                foreignField: '_id',
                as: 'showtimeInfo'
            }
        },
        //unwind showtimeInfo
        {
            $unwind: '$showtimeInfo'
        },
        //filter cinema by id
        {
            $match: {
                'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId)
            }
        }
        // group and total revenue
        ,{
            $group: {
                _id: null,
                revenueToday: {  $sum: '$finalAmount' }
            }
        }
    ])
    return result[0]?.revenueToday || 0;
}


/**
 * Tính tổng doanh thu đồ ăn & thức uống (F&B) ngày hôm nay của một rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Tổng số tiền doanh thu combo bắp nước
 */
const getFoodRevenueToday = async (cinemaId) => {
    const startOfDay = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    const endOfDay = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);
    
    const result = await Booking.aggregate([
        {
            $match: {
                paymentStatus: 'paid',
                createdAt: {
                    $gte: startOfDay,
                    $lte: endOfDay
                }
            }
        },
        {
            $lookup: {
                from: 'showtimes',
                localField: 'showtimeId',
                foreignField: '_id',
                as: 'showtimeInfo'
            }
        },
        {
            $unwind: '$showtimeInfo'
        },
        {
            $match: {
                'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId)
            }
        },
        {
            $group: {
                _id: null,
                foodAndBeverageRevenue: { $sum: '$comboPrice' }
            }
        }
    ]);
    return result[0]?.foodAndBeverageRevenue || 0;
}

/**
 * Tính tổng số lượng vé (ghế) đã bán ra trong ngày hôm nay của một rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Tổng số vé đã bán
 */
const getTicketSoldToday = async (cinemaId) => {
    const startOfDay = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    const endOfDay = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);

    const result = await Booking.aggregate([
        {
            $match: {
                paymentStatus: 'paid',
                createdAt: {
                    $gte: startOfDay,
                    $lte: endOfDay
                }
            }
        },
        {
            $lookup: {
                from: 'showtimes',
                localField: 'showtimeId',
                foreignField: '_id',
                as: 'showtimeInfo'
            }
        },
        {
            $unwind: '$showtimeInfo'
        },
        {
            $match: {
                'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId)
            }
        },
        {
            $group: {
                _id: null,
                ticketsSold: { 
                    $sum: { $size: '$seatIds' } 
                }
            }
        }
    ]);
    return result[0]?.ticketsSold || 0;
}

/**
 * Đếm số lượng bộ phim khác nhau đang có suất chiếu trong ngày hôm nay tại rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Số lượng phim đang chiếu
 */
const getRunningMoviesToday = async (cinemaId) =>{
    const startOfDay = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    const endOfDay = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);

    const uniqueMovieIds = await Showtime.distinct('movieId', {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        startTime: {
            $gte: startOfDay,
            $lte: endOfDay
        },
        isDeleted: false
    });
    return uniqueMovieIds.length
}

/**
 * Đếm số lượng phòng chiếu đang hoạt động (không bị xóa, không bảo trì) tại rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Số lượng phòng đang active
 */
const getActiveScreenToday = async (cinemaId) => {
    return await CinemaRoom.countDocuments({ cinemaId, status: 'active', isDeleted: false });
}

const getAllScreenToday = async (cinemaId) => {
    return await CinemaRoom.countDocuments({ cinemaId, isDeleted: false });
}

/**
 * Tính tổng số ghế có thể bán của tất cả các suất chiếu trong ngày hôm nay tại một rạp cụ thể
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Tổng năng suất ghế (Mẫu số)
 */
const getTotalAvailibleSeatsToday = async (cinemaId) => {
    const startOfDay = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    const endOfDay = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);

    const result = await Showtime.aggregate([
        {
            $match: {
                cinemaId: new mongoose.Types.ObjectId(cinemaId),
                showDate: { $gte: startOfDay, $lte: endOfDay },
                isDeleted: false
            }
        },
        {
            $lookup:{
                from: 'cinema_rooms',
                localField: 'roomId',
                foreignField: '_id',
                as: 'roomInfo'
            }
        },
        {
            $unwind: '$roomInfo'
        },
        {
            $group: {
                _id: null,
                totalSeatsToday: { $sum: '$roomInfo.totalSeats' }
            }
        }
    ])
    return result[0]?.totalSeatsToday || 0;
}

/**
 * Tính tỷ lệ lấp đầy (Occupancy Rate) trong ngày hôm nay của rạp
 * @param {string|mongoose.Types.ObjectId} cinemaId 
 * @returns {Promise<number>} Tỷ lệ % (Ví dụ: 35.5)
 */
const getOccupancyRateToday = async (cinemaId) => {
    const [bookedSeats, totalSeats] = await Promise.all([
        getTicketSoldToday(cinemaId),
        getTotalAvailibleSeatsToday(cinemaId)
    ])
    if (totalSeats === 0) return 0;
    return Number((bookedSeats / totalSeats) * 100).toFixed(2);
}

const getRangeDate = (type) => {
    const now = new Date(new Date().getTime() + 7 * 60 * 60 * 1000); // Đưa về mốc timezone +07 trước
    let start = new Date(now);
    let end = new Date(now);

    switch (type) {
        case 'day':
            start.setHours(0, 0, 0, 0);
            end.setHours(23, 59, 59, 999);
            break;
            
        case 'week': {
            const currentDay = now.getDay();
            const distanceToMonday = currentDay === 0 ? -6 : 1 - currentDay; 
            
            start.setDate(now.getDate() + distanceToMonday);
            start.setHours(0, 0, 0, 0);
            
            end.setDate(start.getDate() + 6);
            end.setHours(23, 59, 59, 999);
            break;
        }
            
        case 'month':
            start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
            end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
            break;
            
        case 'year':
            start = new Date(now.getFullYear(), 0, 1, 0, 0, 0, 0);
            end = new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999);
            break;
            
        default:
            throw new Error("Loại report không hợp lệ!");
    }
    return { start, end };
};

/**
 * Lấy khoảng thời gian của chu kỳ trước (Không làm biến đổi dữ liệu gốc)
 */
const getPreviousRangeDate = (type, currentStart) => {
    let start = new Date(currentStart.getTime());
    let end = new Date(currentStart.getTime());

    switch (type) {
        case 'day':
            start.setDate(start.getDate() - 1);
            end.setDate(end.getDate() - 1);
            end.setHours(23, 59, 59, 999);
            break;
            
        case 'week':
            start.setDate(start.getDate() - 7);
            end.setDate(start.getDate() + 6);
            end.setHours(23, 59, 59, 999);
            break;
            
        case 'month':
            start.setMonth(start.getMonth() - 1);
            start.setDate(1); 
            end = new Date(start.getFullYear(), start.getMonth() + 1, 0, 23, 59, 59, 999);
            break;
            
        case 'year':
            start.setFullYear(start.getFullYear() - 1);
            end = new Date(start.getFullYear(), 11, 31, 23, 59, 59, 999);
            break;
    }
    return { start, end };
};

/**
 * Hàm Helper tính toán % tăng trưởng an toàn
 */
const calculateGrowthRate = (current, previous) => {
    if (!previous || previous === 0) {
        return current > 0 ? 100 : 0; 
    }
    
    const rate = Math.round(((current - previous) / previous) * 100);
    if (rate > 999) return 999; 
    if (rate < -999) return -999;

    return rate;
};

const getRevenueReport = async (cinemaId, reportType = 'day') => {
    const now = new Date(new Date().getTime() + 7 * 60 * 60 * 1000);
    const { start, end } = getRangeDate(reportType);

    const showtimes = await Showtime.aggregate([
        { $match: {  cinemaId: new mongoose.Types.ObjectId(cinemaId),  showDate: { $gte: start, $lte: end },  isDeleted: false } },
        { $sort: { startTime: 1 } },
        { $lookup: { from: 'movies',  localField: 'movieId', foreignField: '_id', as: 'movieInfo'  } },
        { $unwind: { path: '$movieInfo', preserveNullAndEmptyArrays: true } },
        { $lookup: { from: 'cinema_rooms',  localField: 'roomId',  foreignField: '_id', as: 'roomInfo' } },
        { $unwind: { path: '$roomInfo', preserveNullAndEmptyArrays: true } },
        { $lookup: { from: 'bookings', let: { showtimeId: '$_id' },
                pipeline: [
                    {
                        $match: {
                            $expr: {
                                $and: [
                                    { $eq: ['$showtimeId', '$$showtimeId'] },
                                    { $ne: ['$paymentStatus', 'cancel'] }
                                ]
                            }
                        }
                    },
                    { $project: { seatCount: { $size: { $ifNull: ['$seatIds', []] } } } }
                ],
                as: 'bookedTickets'
            }
        },
        {
            $addFields: {
                bookedSeats: { $sum: '$bookedTickets.seatCount' },
                totalSeats: { $ifNull: ['$roomInfo.totalSeats', 0] }
            }
        },
        {
            $addFields: {
                occupancyRate: {
                    $cond: [
                        { $gt: ['$totalSeats', 0] },
                        { $round: [{ $multiply: [{ $divide: ['$bookedSeats', '$totalSeats'] }, 100] }, 0] },
                        0
                    ]
                }
                // Giữ nguyên status logic gốc của bạn
            }
        },
        {
            $project: {
                _id: 0,
                showtimeId: '$_id',
                movieId: { $ifNull: ['$movieInfo._id', '$movieId'] },
                movieName: { $ifNull: ['$movieInfo.movieName', 'N/A'] },
                roomName: { $ifNull: ['$roomInfo.roomName', 'N/A'] },
                startTime: { $dateToString: { format: '%H:%M', date: '$startTime', timezone: '+07:00' } },
                endTime: { $dateToString: { format: '%H:%M', date: '$endTime', timezone: '+07:00' } },
                bookedSeats: 1,
                totalSeats: 1,
                occupancyRate: 1,
                status: {
                    $cond: [
                        { $lt: [now, '$startTime'] },
                        'Coming',
                        {
                            $cond: [
                                { $gt: [now, '$endTime'] },
                                'Done',
                                'Live'
                            ]
                        }
                    ]
                }
            }
        }
    ]);

    return showtimes;
};


/**
 * Daily revenue for each day of the service week
 */
const getWeeklyRevenueChartData = async (cinemaId) => {
    const endOfWeek = new Date(new Date().setHours(23, 59, 59, 999) + 7 * 60 * 60 * 1000);
    const startOfWeek = new Date(new Date().setHours(0, 0, 0, 0) + 7 * 60 * 60 * 1000);
    startOfWeek.setDate(startOfWeek.getDate() - 6);

    const result = await Booking.aggregate([
        { $match: { paymentStatus: 'paid', createdAt: { $gte: startOfWeek, $lte: endOfWeek } } },
        { $lookup: { from: 'showtimes', localField: 'showtimeId', foreignField: '_id', as: 'showtimeInfo' } },
        { $unwind: '$showtimeInfo' },
        { $match: { 'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId) } },
        { $group: { _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt", timezone: "+07:00" } }, totalAmount: { $sum: '$finalAmount' }  } },
        { $sort: { _id: 1 } }
    ]);

    const weeklyReport = [];
    for (let i = 6; i >= 0; i--) {
        const d = new Date(new Date().getTime() + 7 * 60 * 60 * 1000);
        d.setDate(d.getDate() - i);
        const dateStr = d.toLocaleDateString('en-CA'); 
        
        const dayData = result.find(item => item._id === dateStr);
        weeklyReport.push({
            date: dateStr,
            totalAmount: dayData ? dayData.totalAmount : 0
        });
    }
    return weeklyReport;
};

/**
 * Tổng hợp toàn bộ dữ liệu Dashboard cho Manager dựa trên accountId lấy từ JWT
 */
const getManagerDashboard = async (accountId) => {
    const manager = await Manager.findOne({ accountId });
    if(!manager){
        throw new ApiError( StatusCodes.NOT_FOUND, 'Không tìm thấy thông tin Quản lý cho tài khoản này!');
    }

    const cinemaId = manager.cinemaId

    const [
        revenueToday,
        foodRevenueToday,
        ticketSoldToday,
        runningMoviesToday,
        activeScreenToday,
        allScreenToday,
        occupancyRateToday,
        todayShowtimesData,
        weeklyRevenueData
    ] = await Promise.all([
        getRevenueToday(cinemaId),
        getFoodRevenueToday(cinemaId),
        getTicketSoldToday(cinemaId),
        getRunningMoviesToday(cinemaId),
        getActiveScreenToday(cinemaId),
        getAllScreenToday(cinemaId),
        getOccupancyRateToday(cinemaId),
        getRevenueReport(cinemaId),
        getWeeklyRevenueChartData(cinemaId)
    ])

    return {
        managerId: manager._id,
        managerName: manager.fullName,
        cinemaId: cinemaId,
        kpiSummary: {
            revenueToday,
            foodRevenueToday,
            ticketSoldToday,
            runningMoviesToday,
            activeScreenToday,
            allScreenToday,
            occupancyRate: `${occupancyRateToday}%`
        },
        todayShowtimes: todayShowtimesData,
        weeklyRevenue: weeklyRevenueData
    }
}


const getManagerReport = async (accountId, reportType = 'day') => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Không tìm thấy thông tin Quản lý cho tài khoản này!');
    }

    const cinemaId = manager.cinemaId;
    const currentRange = getRangeDate(reportType);
    const previousRange = getPreviousRangeDate(reportType, currentRange.start);

    // Đồng nhất cách tính: Doanh thu vé = finalAmount - comboPrice
    const getStatsByRange = async (start, end) => {
        const stats = await Booking.aggregate([
            { $match: { paymentStatus: 'paid', createdAt: { $gte: start, $lte: end } } },
            { $lookup: { from: 'showtimes', localField: 'showtimeId', foreignField: '_id', as: 'showtimeInfo' } },
            { $unwind: '$showtimeInfo' },
            { $match: { 'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId) } },
            { 
                $group: { 
                    _id: null, 
                    totalTicketRevenue: { $sum: { $subtract: ['$finalAmount', '$comboPrice'] } }, 
                    totalFbRevenue: { $sum: '$comboPrice' }, 
                    totalTicketsSold: { $sum: { $size: '$seatIds' } } 
                } 
            }
        ]);
        
        const res = stats[0] || { totalTicketRevenue: 0, totalFbRevenue: 0, totalTicketsSold: 0 };
        const avg = res.totalTicketsSold > 0 ? Math.round(res.totalTicketRevenue / res.totalTicketsSold) : 0;
        
        return { ...res, avgTicketPrice: avg };
    };

    const getChartDataByRange = async (start, end, types) => {
        let format = "%Y-%m-%d"; 
        if (types === 'day') format = "%H:00"; 
        if (types === 'year') format = "%Y-%m"; 

        const chartStats = await Booking.aggregate([
            { $match: { paymentStatus: 'paid', createdAt: { $gte: start, $lte: end } } },
            { $lookup: { from: 'showtimes', localField: 'showtimeId', foreignField: '_id', as: 'showtimeInfo' } },
            { $unwind: '$showtimeInfo' },
            { $match: { 'showtimeInfo.cinemaId': new mongoose.Types.ObjectId(cinemaId) } },
            {
                $group: {
                    _id: { $dateToString: { format: format, date: "$createdAt", timezone: "+07:00" } },
                    ticketRevenue: { $sum: { $subtract: ['$finalAmount', '$comboPrice'] } }, 
                    fbRevenue: { $sum: '$comboPrice' }
                }
            },
            { $sort: { _id: 1 } }
        ]);

        if (types === 'month') {
            const fullMonthData = [];
            let cursor = new Date(start.getTime());
            while (cursor <= end) {
                const dateStr = cursor.toLocaleDateString('en-CA'); 
                const match = chartStats.find(item => item._id === dateStr);
                
                fullMonthData.push({
                    label: dateStr,
                    ticketRevenue: match ? match.ticketRevenue : 0,
                    fbRevenue: match ? match.fbRevenue : 0
                });
                cursor.setDate(cursor.getDate() + 1);
            }
            return fullMonthData;
        }

        return chartStats.map(item => ({
            label: item._id,
            ticketRevenue: item.ticketRevenue,
            fbRevenue: item.fbRevenue
        }));
    };

    const [currentData, previousData, chartData] = await Promise.all([
        getStatsByRange(currentRange.start, currentRange.end),
        getStatsByRange(previousRange.start, previousRange.end),
        getChartDataByRange(currentRange.start, currentRange.end, reportType)
    ]);

    return {
        ticketRevenue: currentData.totalTicketRevenue,
        fbRevenue: currentData.totalFbRevenue,
        totalTickets: currentData.totalTicketsSold,
        avgTicket: currentData.avgTicketPrice,
        growth: {
            ticket: calculateGrowthRate(currentData.totalTicketRevenue, previousData.totalTicketRevenue),
            fb: calculateGrowthRate(currentData.totalFbRevenue, previousData.totalFbRevenue),
            totalTickets: calculateGrowthRate(currentData.totalTicketsSold, previousData.totalTicketsSold),
            avgTicket: calculateGrowthRate(currentData.avgTicketPrice, previousData.avgTicketPrice)
        },
        chartData: chartData 
    };
};

export const managerDashboardService = {
    getManagerDashboard,
    getTotalAvailibleSeatsToday,
    getManagerReport
}