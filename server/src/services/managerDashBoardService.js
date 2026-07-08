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

    // start of day
    const startOfDay = new Date()
    startOfDay.setHours(0, 0, 0, 0)

    // end of day
    const endOfDay = new Date()
    endOfDay.setHours(23, 59, 59, 999)

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
                // casting to ObjectId
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
    const startOfDay = new Date();
    startOfDay.setHours(0, 0, 0, 0);

    const endOfDay = new Date();
    endOfDay.setHours(23, 59, 59, 999);
    
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
                foodAndBeverageRevenue: { $sum: '$comboPrice' } // Khác biệt duy nhất ở đây!
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
    const startOfDay = new Date();
    startOfDay.setHours(0, 0, 0, 0);
    
    const endOfDay = new Date();
    endOfDay.setHours(23, 59, 59, 999);

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
                    $sum: { $size: '$seatIds' } // Đếm số lượng ghế trong từng booking và cộng dồn lại
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
    const startOfDay = new Date();
    startOfDay.setHours(0, 0, 0, 0);
    
    const endOfDay = new Date();
    endOfDay.setHours(23, 59, 59, 999);

    //distinct để lấy danh sách các movieId không trùng lặp
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
    const startOfDay = new Date();
    startOfDay.setHours(0, 0, 0, 0);

    const endOfDay = new Date();
    endOfDay.setHours(23, 59, 59, 999);

    const result = await Showtime.aggregate([
        {
            $match: {
                cinemaId: new mongoose.Types.ObjectId(cinemaId),
                showDate: { $gte: startOfDay, $lte: endOfDay },
                isDeleted: false
            }
        },
        //join vs cinema_rooms
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
    //đếm vé đã bán (Tử số) và hàm năng suất ghế vừa viết (Mẫu số)
    const [bookedSeats, totalSeats] = await Promise.all([
        getTicketSoldToday(cinemaId),
        getTotalAvailibleSeatsToday(cinemaId)
    ])
    //case: status CinemaRoom = 'mantainance', no showtime today (Mẫu số = 0)
    if (totalSeats === 0) return 0;
    return Number((bookedSeats / totalSeats) * 100).toFixed(2);
}



const getTodayShowtimes = async (cinemaId) => {

    const now = new Date();
    const year = now.getUTCFullYear();
    const month = String(now.getUTCMonth() + 1).padStart(2, '0');
    const day = String(now.getUTCDate()).padStart(2, '0');

    const startOfDay = new Date(`${year}-${month}-${day}T00:00:00.000Z`);
    const endOfDay = new Date(`${year}-${month}-${day}T23:59:59.999Z`);

    // --- TEST FULL PIPELINE CÓ PRESERVE UNWIND ---
    const showtimes = await Showtime.aggregate([
        {
            $match: {
                cinemaId: new mongoose.Types.ObjectId(cinemaId),
                showDate: { $gte: startOfDay, $lte: endOfDay },
                isDeleted: false
            }
        },
        { $sort: { startTime: 1 } },
        {
            $lookup: {
                from: 'movies', 
                localField: 'movieId',
                foreignField: '_id',
                as: 'movieInfo'
            }
        },
        // Chống bốc hơi dữ liệu nếu sai tên collection
        { $unwind: { path: '$movieInfo', preserveNullAndEmptyArrays: true } },

        {
            $lookup: {
                from: 'cinema_rooms', 
                localField: 'roomId',
                foreignField: '_id',
                as: 'roomInfo'
            }
        },
        { $unwind: { path: '$roomInfo', preserveNullAndEmptyArrays: true } },

        {
            $lookup: {
                from: 'bookings',
                let: { showtimeId: '$_id' },
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
                },
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
                status: 1
            }
        }
    ]);

    return showtimes;
};
/**
 * Tổng hợp toàn bộ dữ liệu Dashboard cho Manager dựa trên accountId lấy từ JWT
 * @param {string} accountId 
 * @returns {Promise<Object>} Object chứa thông tin Manager và các KPI hôm nay
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
        todayShowtimes
    ] = await Promise.all([
        getRevenueToday(cinemaId),
        getFoodRevenueToday(cinemaId),
        getTicketSoldToday(cinemaId),
        getRunningMoviesToday(cinemaId),
        getActiveScreenToday(cinemaId),
        getAllScreenToday(cinemaId),
        getOccupancyRateToday(cinemaId),
        getTodayShowtimes(cinemaId)
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
        todayShowtimes: todayShowtimes
    }

}


export const managerDashboardService = {
    getManagerDashboard,
    getTotalAvailibleSeatsToday
}