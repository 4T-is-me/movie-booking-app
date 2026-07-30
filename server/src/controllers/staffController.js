import { staffService } from "../services/staffService.js";
import { StatusCodes } from "http-status-codes";

const checkInTicket = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const { bookingCode } = req.body;

        const result = await staffService.checkInTicket(accountId, { bookingCode });

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Ticket checked in successfully.",
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const getMyRatings = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const result = await staffService.getMyRatings(accountId);

        return res.status(StatusCodes.OK).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

export const staffController = {
    checkInTicket,
    getMyRatings
};
