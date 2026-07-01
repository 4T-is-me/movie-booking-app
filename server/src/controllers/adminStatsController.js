import { StatusCodes } from 'http-status-codes';
import * as adminStatsService from '../services/adminStatsService.js';

// GET /api/admin/stats/overview
export const getOverviewStats = async (req, res, next) => {
    try {
        const result = await adminStatsService.getOverviewStats();
        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/stats/movie-revenue
export const getMovieRevenueStats = async (req, res, next) => {
    try {
        // Placeholder response
        return res.status(StatusCodes.OK).json({
            success: true,
            data: [],
            message: "Movie revenue stats placeholder"
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/stats/revenue/daily
export const getDailyRevenue = async (req, res, next) => {
    try {
        const { startDate, endDate } = req.query;
        const result = await adminStatsService.getDailyRevenueStats(startDate, endDate);
        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/stats/revenue/monthly
export const getMonthlyRevenue = async (req, res, next) => {
    try {
        const { startDate, endDate } = req.query;
        const result = await adminStatsService.getMonthlyRevenueStats(startDate, endDate);
        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/stats/revenue/by-movie
export const getRevenueByMovie = async (req, res, next) => {
    try {
        const result = await adminStatsService.getMovieRevenueStats();
        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};
