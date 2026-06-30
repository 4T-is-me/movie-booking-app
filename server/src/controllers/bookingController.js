import { bookingService } from "../services/bookingService.js";
import { StatusCodes } from "http-status-codes";
import ApiError from '../utils/ApiError.js';

const handleHoldSeats = async (req, res, next) => {
    try {

        const showtimeId = req.params.id

        const {  seatIds } = req.body;
        const result = await bookingService.handleHoldSeats({
            accountId: req.user?.accountId,
            showtimeId,
            seatIds
        })
        return res.status(StatusCodes.OK).json({success: true,data: result})
    } catch (error) {
        next(error)
    }
}

const handleConfirmBooking = async (req, res, next) => {
    try {

        const {showtimeId, seatIds, useScore, paymentMethod, promotionId ,combos} = req.body;

        const result = await bookingService.handleConfirmBooking({
            accountId: req.user?.accountId,
            showtimeId,
            seatIds,
            useScore,
            paymentMethod,
            promotionId,
            combos
        });

        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });

    } catch (error) {
        next(error);
    }
};

const handleGetHistory = async (req, res, next) => {
    try {
        const page = Number(req.query.page) || 1;
        const limit = Number(req.query.limit) || 10;

        const result = await bookingService.handleGetHistory(req.user?.accountId, page, limit)
        return res.status(StatusCodes.OK).json({success: true,data: result})
    } catch (error) {
        next(error)
    }
}

const handleGetScoreHistory = async (req, res, next) => {
    try {
        const result = await bookingService.handleGetScoreHistory(req.user?.accountId)
        return res.status(StatusCodes.OK).json({success: true,data: result})
    } catch (error) {
        next(error)
    }
}
const handleReleaseBooking = async (req, res, next) => {
    try {
        const showtimeId = req.params.id;
        const accountId = req.user?.accountId;

        if (!accountId) {
            throw new ApiError(StatusCodes.UNAUTHORIZED, "Account not found in token");
        }

        const result = await bookingService.handleReleaseBooking({
            accountId,
            showtimeId
        });

        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
}
export const bookingController = { 
    handleHoldSeats,
    handleConfirmBooking,
    handleGetHistory,
    handleGetScoreHistory,
    handleReleaseBooking
}
