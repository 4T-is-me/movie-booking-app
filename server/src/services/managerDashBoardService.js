import { CinemaRoom } from "../models/cinemaRoomModel.js";
import { Showtime } from "../models/showTimeModel.js";
import { Manager } from "../models/managerModel.js";
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
    throw new ApiError(
      StatusCodes.NOT_FOUND,
      "Không tìm thấy thông tin Quản lý cho tài khoản này!",
    );
  }
  return manager;
};

/**
 * Lấy khoảng thời gian của một chu kỳ (Không dùng múi giờ add +7)
 */
const getRangeDate = (type) => {
  const now = new Date();
  let start = new Date(now);
  let end = new Date(now);

  switch (type) {
    case "day":
      start.setHours(0, 0, 0, 0);
      end.setHours(23, 59, 59, 999);
      break;

    case "week": {
      const currentDay = now.getDay();
      const distanceToMonday = currentDay === 0 ? -6 : 1 - currentDay;

      start.setDate(now.getDate() + distanceToMonday);
      start.setHours(0, 0, 0, 0);

      end = new Date(start);
      end.setDate(start.getDate() + 6);
      end.setHours(23, 59, 59, 999);
      break;
    }

    case "month":
      start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
      end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
      break;

    case "year":
      start = new Date(now.getFullYear(), 0, 1, 0, 0, 0, 0);
      end = new Date(now.getFullYear(), 11, 31, 23, 59, 59, 999);
      break;

    default:
      throw new Error("Loại report không hợp lệ!");
  }
  return { start, end };
};

/**
 * Lấy khoảng thời gian đã trôi qua trong chu kỳ hiện tại (từ đầu chu kỳ đến hiện tại)
 */
const getCurrentElapsedRange = (type, now) => {
  let start = new Date(now);
  let end = new Date(now);

  switch (type) {
    case "day":
      start.setHours(0, 0, 0, 0);
      break;

    case "week": {
      const currentDay = now.getDay();
      const distanceToMonday = currentDay === 0 ? -6 : 1 - currentDay;
      start.setDate(now.getDate() + distanceToMonday);
      start.setHours(0, 0, 0, 0);
      break;
    }

    case "month":
      start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
      break;

    case "year":
      start = new Date(now.getFullYear(), 0, 1, 0, 0, 0, 0);
      break;
  }
  return { start, end };
};

/**
 * Lấy khoảng thời gian tương ứng của chu kỳ trước (tương ứng với thời điểm hiện tại của chu kỳ này)
 */
const getPreviousElapsedRange = (type, now) => {
  let start = new Date(now);
  let end = new Date(now);

  switch (type) {
    case "day":
      start.setDate(start.getDate() - 1);
      start.setHours(0, 0, 0, 0);
      end.setDate(end.getDate() - 1);
      break;

    case "week": {
      const currentDay = now.getDay();
      const distanceToMonday = currentDay === 0 ? -6 : 1 - currentDay;
      const currentStart = new Date(now);
      currentStart.setDate(now.getDate() + distanceToMonday);
      currentStart.setHours(0, 0, 0, 0);

      start = new Date(currentStart);
      start.setDate(start.getDate() - 7);

      end = new Date(now);
      end.setDate(end.getDate() - 7);
      break;
    }

    case "month": {
      start = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0, 0);
      const lastDayOfPrevMonth = new Date(
        now.getFullYear(),
        now.getMonth(),
        0,
      ).getDate();
      const prevDay = Math.min(now.getDate(), lastDayOfPrevMonth);
      end = new Date(
        now.getFullYear(),
        now.getMonth() - 1,
        prevDay,
        now.getHours(),
        now.getMinutes(),
        now.getSeconds(),
        now.getMilliseconds(),
      );
      break;
    }

    case "year": {
      start = new Date(now.getFullYear() - 1, 0, 1, 0, 0, 0, 0);
      const isLeap = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
      let prevDay = now.getDate();
      let prevMonth = now.getMonth();
      if (prevMonth === 1 && prevDay === 29 && !isLeap(now.getFullYear() - 1)) {
        prevDay = 28;
      }
      end = new Date(
        now.getFullYear() - 1,
        prevMonth,
        prevDay,
        now.getHours(),
        now.getMinutes(),
        now.getSeconds(),
        now.getMilliseconds(),
      );
      break;
    }
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

/**
 * Tính toán thống kê theo khoảng thời gian dựa trên Showtime.showDate
 */
const getStatsByRange = async (cinemaId, start, end) => {
  const stats = await Showtime.aggregate([
    {
      $match: {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        showDate: { $gte: start, $lte: end },
        isDeleted: false,
      },
    },
    {
      $lookup: {
        from: "bookings",
        localField: "_id",
        foreignField: "showtimeId",
        as: "bookingInfo",
      },
    },
    { $unwind: "$bookingInfo" },
    {
      $match: {"bookingInfo.paymentStatus": "paid"},
    },
    {
      $group: {
        _id: null,
        totalRevenue: { $sum: "$bookingInfo.finalAmount" },
        totalTicketRevenue: {
          $sum: {
            $subtract: ["$bookingInfo.finalAmount", "$bookingInfo.comboPrice"],
          },
        },
        totalFbRevenue: { $sum: "$bookingInfo.comboPrice" },
        totalTicketsSold: { $sum: { $size: "$bookingInfo.seatIds" } },
      },
    },
  ]);

  const res = stats[0] || {
    totalRevenue: 0,
    totalTicketRevenue: 0,
    totalFbRevenue: 0,
    totalTicketsSold: 0,
  };
  const avg = res.totalTicketsSold > 0
      ? Math.round(res.totalTicketRevenue / res.totalTicketsSold)
      : 0;

  return { ...res, avgTicketPrice: avg };
};

/**
 * Lấy danh sách suất chiếu hôm nay phục vụ hiển thị trên Dashboard
 */
const getRevenueReport = async (cinemaId, reportType = "day") => {
  const now = new Date();
  const { start, end } = getRangeDate(reportType);

  const showtimes = await Showtime.aggregate([
    {
      $match: {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        showDate: { $gte: start, $lte: end },
        isDeleted: false,
      },
    },
    { $sort: { startTime: 1 } },
    {
      $lookup: {
        from: "movies",
        localField: "movieId",
        foreignField: "_id",
        as: "movieInfo",
      },
    },
    { $unwind: { path: "$movieInfo", preserveNullAndEmptyArrays: true } },
    {
      $lookup: {
        from: "cinema_rooms",
        localField: "roomId",
        foreignField: "_id",
        as: "roomInfo",
      },
    },
    { $unwind: { path: "$roomInfo", preserveNullAndEmptyArrays: true } },
    {
      $lookup: {
        from: "bookings",
        let: { showtimeId: "$_id" },
        pipeline: [
          {
            $match: {
              $expr: {
                $and: [
                  { $eq: ["$showtimeId", "$$showtimeId"] },
                  { $ne: ["$paymentStatus", "cancel"] },
                ],
              },
            },
          },
          { $project: { seatCount: { $size: { $ifNull: ["$seatIds", []] } } } },
        ],
        as: "bookedTickets",
      },
    },
    {
      $addFields: {
        bookedSeats: { $sum: "$bookedTickets.seatCount" },
        totalSeats: { $ifNull: ["$roomInfo.totalSeats", 0] },
      },
    },
    {
      $addFields: {
        occupancyRate: {
          $cond: [
            { $gt: ["$totalSeats", 0] },
            {
              $round: [
                {$multiply: [{ $divide: ["$bookedSeats", "$totalSeats"] }, 100,],},0,
              ],
            },0,
          ], 
        },
      },
    },
    {
      $project: {
        _id: 0,
        showtimeId: "$_id",
        movieId: { $ifNull: ["$movieInfo._id", "$movieId"] },
        movieName: { $ifNull: ["$movieInfo.movieName", "N/A"] },
        roomName: { $ifNull: ["$roomInfo.roomName", "N/A"] },
        startTime: {
          $dateToString: {
            format: "%H:%M",
            date: "$startTime",
            timezone: "+07:00",
          },
        },
        endTime: {
          $dateToString: {
            format: "%H:%M",
            date: "$endTime",
            timezone: "+07:00",
          },
        },
        bookedSeats: 1,
        totalSeats: 1,
        occupancyRate: 1,
        status: {
          $cond: [
            { $lt: [now, "$startTime"] },
            "Coming",
            {
              $cond: [{ $gt: [now, "$endTime"] }, "Done", "Live"],
            },
          ],
        },
      },
    },
  ]);

  return showtimes;
};

/**
 * Lấy dữ liệu biểu đồ doanh thu tuần phục vụ hiển thị trên Dashboard (7 ngày gần nhất)
 */
const getWeeklyRevenueChartData = async (cinemaId) => {
  const endOfWeek = new Date();
  endOfWeek.setHours(23, 59, 59, 999);

  const startOfWeek = new Date();
  startOfWeek.setDate(startOfWeek.getDate() - 6);
  startOfWeek.setHours(0, 0, 0, 0);

  const result = await Showtime.aggregate([
    {
      $match: {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        showDate: { $gte: startOfWeek, $lte: endOfWeek },
        isDeleted: false,
      },
    },
    {
      $lookup: {
        from: "bookings",
        localField: "_id",
        foreignField: "showtimeId",
        as: "bookingInfo",
      },
    },
    { $unwind: "$bookingInfo" },
    {
      $match: {
        "bookingInfo.paymentStatus": "paid",
      },
    },
    {
      $group: {
        _id: {
          $dateToString: {
            format: "%Y-%m-%d",
            date: "$showDate",
            timezone: "+07:00",
          },
        },
        totalAmount: { $sum: "$bookingInfo.finalAmount" },
      },
    },
    { $sort: { _id: 1 } },
  ]);

  const weeklyReport = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const dateStr = d.toLocaleDateString("en-CA");

    const dayData = result.find((item) => item._id === dateStr);
    weeklyReport.push({
      date: dateStr,
      totalAmount: dayData ? dayData.totalAmount : 0,
    });
  }
  return weeklyReport;
};

/**
 * Đếm số lượng bộ phim khác nhau đang có suất chiếu trong ngày hôm nay tại rạp cụ thể
 */
const getRunningMoviesToday = async (cinemaId) => {
  const { start, end } = getRangeDate("day");

  const uniqueMovieIds = await Showtime.distinct("movieId", {
    cinemaId: new mongoose.Types.ObjectId(cinemaId),
    showDate: {
      $gte: start,
      $lte: end,
    },
    isDeleted: false,
  });
  return uniqueMovieIds.length;
};

/**
 * Đếm số lượng phòng chiếu đang hoạt động tại rạp cụ thể
 */
const getActiveScreenToday = async (cinemaId) => {
  return await CinemaRoom.countDocuments({
    cinemaId,
    status: "active",
    isDeleted: false,
  });
};

/**
 * Đếm tổng số lượng phòng chiếu của rạp
 */
const getAllScreenToday = async (cinemaId) => {
  return await CinemaRoom.countDocuments({ cinemaId, isDeleted: false });
};

/**
 * Tính tổng số ghế có thể bán của tất cả các suất chiếu trong ngày hôm nay tại một rạp cụ thể
 */
const getTotalAvailibleSeatsToday = async (cinemaId) => {
  const { start, end } = getRangeDate("day");

  const result = await Showtime.aggregate([
    {
      $match: {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        showDate: { $gte: start, $lte: end },
        isDeleted: false,
      },
    },
    {
      $lookup: {
        from: "cinema_rooms",
        localField: "roomId",
        foreignField: "_id",
        as: "roomInfo",
      },
    },
    {
      $unwind: "$roomInfo",
    },
    {
      $group: {
        _id: null,
        totalSeatsToday: { $sum: "$roomInfo.totalSeats" },
      },
    },
  ]);
  return result[0]?.totalSeatsToday || 0;
};

/**
 * Tổng hợp toàn bộ dữ liệu Dashboard cho Manager dựa trên accountId lấy từ JWT
 */
const getManagerDashboard = async (accountId) => {
  const manager = await Manager.findOne({ accountId });
  if (!manager) {
    throw new ApiError(
      StatusCodes.NOT_FOUND,
      "Không tìm thấy thông tin Quản lý cho tài khoản này!",
    );
  }

  const cinemaId = manager.cinemaId;
  const { start: startOfDay, end: endOfDay } = getRangeDate("day");

  const [
    todayStats,
    runningMoviesToday,
    activeScreenToday,
    allScreenToday,
    totalSeatsToday,
    todayShowtimesData,
    weeklyRevenueData,
  ] = await Promise.all([
    getStatsByRange(cinemaId, startOfDay, endOfDay),
    getRunningMoviesToday(cinemaId),
    getActiveScreenToday(cinemaId),
    getAllScreenToday(cinemaId),
    getTotalAvailibleSeatsToday(cinemaId),
    getRevenueReport(cinemaId),
    getWeeklyRevenueChartData(cinemaId),
  ]);

  const bookedSeats = todayStats.totalTicketsSold;
  const occupancyRateToday =
    totalSeatsToday === 0
      ? 0
      : Number((bookedSeats / totalSeatsToday) * 100).toFixed(2);

  return {
    managerId: manager._id,
    managerName: manager.fullName,
    cinemaId: cinemaId,
    kpiSummary: {
      revenueToday: todayStats.totalRevenue,
      foodRevenueToday: todayStats.totalFbRevenue,
      ticketSoldToday: todayStats.totalTicketsSold,
      runningMoviesToday,
      activeScreenToday,
      allScreenToday,
      occupancyRate: `${occupancyRateToday}%`,
    },
    todayShowtimes: todayShowtimesData,
    weeklyRevenue: weeklyRevenueData,
  };
};

/**
 * Lấy dữ liệu biểu đồ doanh thu theo chu kỳ phục vụ Manager Report
 */
const getChartDataByRange = async (cinemaId, start, end, types) => {
  let format = "%Y-%m-%d";
  if (types === "day") format = "%H:00";
  if (types === "year") format = "%Y-%m";

  const chartStats = await Showtime.aggregate([
    {
      $match: {
        cinemaId: new mongoose.Types.ObjectId(cinemaId),
        showDate: { $gte: start, $lte: end },
        isDeleted: false,
      },
    },
    {
      $lookup: {
        from: "bookings",
        localField: "_id",
        foreignField: "showtimeId",
        as: "bookingInfo",
      },
    },
    { $unwind: "$bookingInfo" },
    {
      $match: {
        "bookingInfo.paymentStatus": "paid",
      },
    },
    {
      $group: {
        _id: {
          $dateToString: {
            format: format,
            date: types === "day" ? "$startTime" : "$showDate",
            timezone: "+07:00",
          },
        },
        ticketRevenue: {
          $sum: {
            $subtract: ["$bookingInfo.finalAmount", "$bookingInfo.comboPrice"],
          },
        },
        fbRevenue: { $sum: "$bookingInfo.comboPrice" },
      },
    },
    { $sort: { _id: 1 } },
  ]);

  if (types === "day") {
    const fullDayData = [];
    for (let h = 0; h < 24; h++) {
      const hourStr = String(h).padStart(2, "0") + ":00";
      const match = chartStats.find((item) => item._id === hourStr);
      fullDayData.push({
        label: hourStr,
        ticketRevenue: match ? match.ticketRevenue : 0,
        fbRevenue: match ? match.fbRevenue : 0,
      });
    }
    return fullDayData;
  }

  if (types === "week") {
    const fullWeekData = [];
    let cursor = new Date(start);
    for (let i = 0; i < 7; i++) {
      const dateStr = cursor.toLocaleDateString("en-CA");
      const match = chartStats.find((item) => item._id === dateStr);

      fullWeekData.push({
        label: dateStr,
        ticketRevenue: match ? match.ticketRevenue : 0,
        fbRevenue: match ? match.fbRevenue : 0,
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    return fullWeekData;
  }

  if (types === "month") {
    const fullMonthData = [];
    let cursor = new Date(start.getTime());
    while (cursor <= end) {
      const dateStr = cursor.toLocaleDateString("en-CA");
      const match = chartStats.find((item) => item._id === dateStr);

      fullMonthData.push({
        label: dateStr,
        ticketRevenue: match ? match.ticketRevenue : 0,
        fbRevenue: match ? match.fbRevenue : 0,
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    return fullMonthData;
  }

  if (types === "year") {
    const fullYearData = [];
    const yearStr = start.getFullYear();
    for (let m = 0; m < 12; m++) {
      const monthStr = `${yearStr}-${String(m + 1).padStart(2, "0")}`;
      const match = chartStats.find((item) => item._id === monthStr);
      fullYearData.push({
        label: monthStr,
        ticketRevenue: match ? match.ticketRevenue : 0,
        fbRevenue: match ? match.fbRevenue : 0,
      });
    }
    return fullYearData;
  }

  return [];
};
/**
 * hàm lấy 5 bộ phim có số lượng vé bán nhiều nhất(day, week, month, year)
 */
const getTopMoviesByRange = async (cinemaId, start, end, limit = 5) => {
    const topMovies = await Showtime.aggregate([
      {
        $match: {
          cinemaId: new mongoose.Types.ObjectId(cinemaId),
          showDate: { $gte: start, $lte: end },
          isDeleted: false,
        },
      },
      {
        $lookup: {
          from: "bookings",
          localField: "_id",
          foreignField: "showtimeId",
          as: "bookingInfo",
        },
      },
      { $unwind: "$bookingInfo" },
      {
        $match: {
          "bookingInfo.paymentStatus": "paid",
        },
      },
      {
        $group: {
          _id: "$movieId", 
          totalTickets: { 
            $sum: { $size: { $ifNull: ["$bookingInfo.seatIds", []] } }
          }
        },
      },
      {
        $lookup: {
          from: "movies", 
          localField: "_id",
          foreignField: "_id",
          as: "movieDetails",
        },
      },
      { $unwind: "$movieDetails" },
      {
        $project: {
          _id: 1,
          title: "$movieDetails.movieName", 
          totalTickets: 1,
        },
      },
      { $sort: { totalTickets: -1 } },
      { $limit: limit }
    ])
    return topMovies;
}



/**
 * Lấy dữ liệu báo cáo Manager Report (Day, Week, Month, Year) bao gồm doanh thu, số vé, tăng trưởng và biểu đồ
 */
const getManagerReport = async (accountId, reportType = "day") => {
  const manager = await Manager.findOne({ accountId });
  if (!manager) {
    throw new ApiError(
      StatusCodes.NOT_FOUND,
      "Không tìm thấy thông tin Quản lý cho tài khoản này!",
    );
  }

  const cinemaId = manager.cinemaId;
  const now = new Date();

  // Ranges
  const currentRange = getRangeDate(reportType);
  const currentElapsedRange = getCurrentElapsedRange(reportType, now);
  const previousElapsedRange = getPreviousElapsedRange(reportType, now);

  const [currentData, currentElapsedData, previousElapsedData, chartData, topMovies] =
    await Promise.all([
      getStatsByRange(cinemaId, currentRange.start, currentRange.end),
      getStatsByRange(cinemaId, currentElapsedRange.start, currentElapsedRange.end),
      getStatsByRange(cinemaId, previousElapsedRange.start, previousElapsedRange.end),
      getChartDataByRange(cinemaId, currentRange.start, currentRange.end, reportType),
      getTopMoviesByRange(cinemaId, currentRange.start, currentRange.end, 5)
    ]);

  return {
    ticketRevenue: currentData.totalTicketRevenue,
    fbRevenue: currentData.totalFbRevenue,
    totalTickets: currentData.totalTicketsSold,
    avgTicket: currentData.avgTicketPrice,
    growth: {
      ticket: calculateGrowthRate( currentElapsedData.totalTicketRevenue, previousElapsedData.totalTicketRevenue),
      fb: calculateGrowthRate( currentElapsedData.totalFbRevenue, previousElapsedData.totalFbRevenue ),
      totalTickets: calculateGrowthRate( currentElapsedData.totalTicketsSold, previousElapsedData.totalTicketsSold),
      avgTicket: calculateGrowthRate( currentElapsedData.avgTicketPrice, previousElapsedData.avgTicketPrice ),
    },
    chartData: chartData,
    topMovies: topMovies
  };
};

export const managerDashboardService = {
  getManagerDashboard,
  getTotalAvailibleSeatsToday,
  getManagerReport
};
