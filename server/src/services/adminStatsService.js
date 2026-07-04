import { Booking } from '../models/bookingModel.js';
import { Account } from '../models/accountModel.js';
import { Role } from '../models/rolesModel.js';
import { Movie } from '../models/movieModel.js';

export const getDailyRevenueStats = async (startDate, endDate) => {
    const matchStage = {};

    if (startDate || endDate) {
        matchStage.createdAt = {};
        if (startDate) {
            matchStage.createdAt.$gte = new Date(startDate);
        }
        if (endDate) {
            const end = new Date(endDate);
            end.setHours(23, 59, 59, 999);
            matchStage.createdAt.$lte = end;
        }
    }

    // Lọc theo trạng thái hóa đơn hoạt động (ở model invoice là status = 1)
    matchStage.status = 1;

    const pipeline = [
        { $match: matchStage },
        {
            $group: {
                _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
                totalRevenue: { $sum: "$totalMoney" }, // Sử dụng totalMoney của Invoice
                ticketCount: { $sum: 1 }
            }
        },
        {
            $project: {
                _id: 0,
                date: "$_id",
                totalRevenue: 1,
                ticketCount: 1
            }
        },
        { $sort: { date: 1 } }
    ];

    return await Invoice.aggregate(pipeline);
};

export const getMonthlyRevenueStats = async (startDate, endDate) => {
    const matchStage = {};

    if (startDate || endDate) {
        matchStage.createdAt = {};
        if (startDate) {
            matchStage.createdAt.$gte = new Date(startDate);
        }
        if (endDate) {
            const end = new Date(endDate);
            end.setHours(23, 59, 59, 999);
            matchStage.createdAt.$lte = end;
        }
    }

    // Lọc theo trạng thái hóa đơn hoạt động (ở model invoice là status = 1)
    matchStage.status = 1;

    const pipeline = [
        { $match: matchStage },
        {
            $group: {
                _id: {
                    year: { $year: "$createdAt" },
                    month: { $month: "$createdAt" }
                },
                totalRevenue: { $sum: "$totalMoney" }, // Sử dụng totalMoney của Invoice
                ticketCount: { $sum: 1 }
            }
        },
        {
            $project: {
                _id: 0,
                year: "$_id.year",
                month: "$_id.month",
                totalRevenue: 1,
                ticketCount: 1
            }
        },
        { $sort: { year: 1, month: 1 } }
    ];

    return await Invoice.aggregate(pipeline);
};

export const getMovieRevenueStats = async () => {
    const pipeline = [
        {
            $group: {
                _id: "$movieId",
                totalRevenue: { $sum: "$totalMoney" }
            }
        },
        {
            $lookup: {
                from: "movies",
                localField: "_id",
                foreignField: "_id",
                as: "movie"
            }
        },
        {
            $unwind: {
                path: "$movie",
                preserveNullAndEmptyArrays: true
            }
        },
        {
            $project: {
                _id: 0,
                movieId: "$_id",
                totalRevenue: 1,
                movieName: { $ifNull: ["$movie.movieName", "$movie.movieNameVN", "Unknown Movie"] },
                movieNameVN: { $ifNull: ["$movie.movieNameVN", "$movie.movieName", "Unknown Movie"] }
            }
        },
        { $sort: { totalRevenue: -1 } },
        { $limit: 10 }
    ];

    return await Invoice.aggregate(pipeline);
};

export const getOverviewStats = async () => {
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);

    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const endOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999);

    // 1. Tìm role Member trước để dùng trong query Account
    const memberRole = await Role.findOne({
        $or: [
            { name: 'Member' },
            { roleName: 'Member' }
        ]
    });

    // 2. Chạy song song 4 query bằng Promise.all
    const [revenueData, invoicesToday, memberCount, moviesShowingCount] = await Promise.all([
        // (1) Doanh thu tháng hiện tại từ Invoice
        Invoice.aggregate([
            {
                $match: {
                    createdAt: { $gte: startOfMonth, $lte: endOfMonth }
                }
            },
            {
                $group: {
                    _id: null,
                    total: { $sum: "$totalMoney" }
                }
            }
        ]),
        // (2) Lấy các invoice bán hôm nay để tính số vé
        Invoice.find({
            createdAt: { $gte: startOfDay, $lte: endOfDay }
        }).select('seat'),
        // (3) Số Member status=1 từ Account
        memberRole ? Account.countDocuments({ roleId: memberRole._id, status: 1 }) : 0,
        // (4) Số phim isDeleted=false đang chiếu từ Movie (dùng Index đã thêm ở ISSUE_30)
        Movie.countDocuments({
            isDeleted: false,
            releaseDate: { $lte: now },
            endDate: { $gte: now }
        })
    ]);

    // Tính tổng doanh thu tháng hiện tại
    const currentMonthRevenue = revenueData[0]?.total || 0;

    // Tính số vé bán hôm nay từ danh sách ghế (mỗi ghế là 1 vé)
    let ticketsSoldToday = 0;
    invoicesToday.forEach(invoice => {
        if (invoice.seat) {
            const seatsArray = invoice.seat.split(',').map(s => s.trim()).filter(Boolean);
            ticketsSoldToday += seatsArray.length;
        }
    });

    const result = {
        totalRevenue: currentMonthRevenue,
        totalTicketsSold: ticketsSoldToday,
        totalMembers: memberCount,
        totalMovies: moviesShowingCount
    };

    return result;
};
