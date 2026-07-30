import { bookingService } from "../services/bookingService.js";
import { StatusCodes } from "http-status-codes";
import ApiError from '../utils/ApiError.js';


// POST /api/showtimes/:id/hold - Hold a seat for a booking
const handleHoldSeats = async (req, res, next) => {
    try {

        const showtimeId = req.params.id

        const { seatIds } = req.body;
        const result = await bookingService.handleHoldSeats({
            accountId: req.user?.accountId,
            showtimeId,
            seatIds
        })
        return res.status(StatusCodes.OK).json({ success: true, data: result })
    } catch (error) {
        next(error)
    }
}

// POST /api/booking/checkout --check seats valid + total price + check and deduct points
const handleConfirmBooking = async (req, res, next) => {
    try {

        const { showtimeId, seatIds, useScore, useRewardPoints, paymentMethod, couponCode, promotionId, combos, scoreToRedeem, memberId, isGuestCheckout } = req.body;

        const activeUseScore = useRewardPoints !== undefined ? useRewardPoints : useScore;

        const result = await bookingService.handleConfirmBooking({
            accountId: req.user?.accountId,
            showtimeId,
            seatIds,
            useScore: activeUseScore,
            paymentMethod,
            couponCode,
            promotionId,
            combos,
            scoreToRedeem,
            memberId,
            isGuestCheckout: isGuestCheckout === true || isGuestCheckout === 'true'
        });

        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });

    } catch (error) {
        next(error);
    }
};
// GET /api/booking/history --get history
const handleGetHistory = async (req, res, next) => {
    try {
        const page = Number(req.query.page) || 1;
        const limit = Number(req.query.limit) || 10;

        const result = await bookingService.handleGetHistory(req.user?.accountId, page, limit)
        return res.status(StatusCodes.OK).json({ success: true, data: result })
    } catch (error) {
        next(error)
    }
}
// GET /api/booking/my-tickets --get active tickets
const handleGetMyTickets = async (req, res, next) => {
    try {
        const page = Number(req.query.page) || 1;
        const limit = Number(req.query.limit) || 10;

        const result = await bookingService.handleGetMyTickets(req.user?.accountId, page, limit)
        return res.status(StatusCodes.OK).json({ success: true, data: result })
    } catch (error) {
        next(error)
    }
}
// GET /api/booking/score-history --get score history
const handleGetScoreHistory = async (req, res, next) => {
    try {
        const result = await bookingService.handleGetScoreHistory(req.user?.accountId)
        return res.status(StatusCodes.OK).json({ success: true, data: result })
    } catch (error) {
        next(error)
    }
}
// POST /api/showtimes/:id/release - Release a seat for a booking
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
// POST /api/booking/:id - get booking detail
const handleGetBookingDetail = async (req, res, next) => {

    try {
        const bookingId = req.params.id;
        const accountId = req.user?.accountId;
        //validate input
        if (!accountId || !bookingId) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "Missing accountId or bookingId");
        }
        const result = await bookingService.handleGetbookingDetail(bookingId, accountId);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }

}

const handleRateStaff = async (req, res, next) => {
    try {
        const bookingId = req.params.id;
        const { rating, comment } = req.body;
        const accountId = req.user?.accountId;

        if (!rating || rating < 1 || rating > 5) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "Invalid rating. Must be between 1 and 5.");
        }

        const result = await bookingService.handleRateStaff(bookingId, accountId, rating, comment);
        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Staff rated successfully",
            data: result
        });
    } catch (error) {
        next(error);
    }
}

export const bookingController = {
    handleHoldSeats,
    handleConfirmBooking,
    handleGetHistory,
    handleGetScoreHistory,
    handleReleaseBooking,
    handleGetBookingDetail,
    handleGetMyTickets,
    handleRateStaff
}
