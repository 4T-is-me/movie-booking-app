import { Booking } from '../models/bookingModel.js';
import { Invoice } from '../models/invoiceModel.js';
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

    // Filter by active invoice status (in invoice model, status = 1)
    matchStage.status = 1;

    const pipeline = [
        { $match: matchStage },
        {
            $group: {
                _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
                totalRevenue: { $sum: "$totalMoney" }, // Use totalMoney of Invoice
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

    // Filter by active invoice status (in invoice model, status = 1)
    matchStage.status = 1;

    const pipeline = [
        { $match: matchStage },
        {
            $group: {
                _id: {
                    year: { $year: "$createdAt" },
                    month: { $month: "$createdAt" }
                },
                totalRevenue: { $sum: "$totalMoney" }, // Use totalMoney of Invoice
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

    // 1. Find Member role first to use in Account query
    const memberRole = await Role.findOne({
        $or: [
            { name: 'Member' },
            { roleName: 'Member' }
        ]
    });

    // 2. Run 4 queries in parallel using Promise.all
    const [revenueData, invoicesToday, memberCount, moviesShowingCount] = await Promise.all([
        // (1) Current month revenue from Invoice
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
        // (2) Get invoices sold today to calculate number of tickets
        Invoice.find({
            createdAt: { $gte: startOfDay, $lte: endOfDay }
        }).select('seat'),
        // (3) Number of active Members (status = 1) from Account
        memberRole ? Account.countDocuments({ roleId: memberRole._id, status: 1 }) : 0,
        // (4) Number of active showing movies (isDeleted = false) from Movie
        Movie.countDocuments({
            isDeleted: false,
            releaseDate: { $lte: now },
            endDate: { $gte: now }
        })
    ]);

    // Calculate total revenue of the current month
    const currentMonthRevenue = revenueData[0]?.total || 0;

    // Calculate tickets sold today from seat list (each seat is 1 ticket)
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
