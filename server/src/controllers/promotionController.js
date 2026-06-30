import { Promotion } from "../models/promotionModel.js";
import { StatusCodes } from "http-status-codes";

const applyPromotion = async (req, res, next) => {
    try {
        const { code } = req.body;
        if (!code) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Coupon code is required"
            });
        }

        const now = new Date();
        const promotion = await Promotion.findOne({
            title: code.trim().toUpperCase(),
            startTime: { $lte: now },
            endTime: { $gte: now }
        });

        if (!promotion) {
            return res.status(StatusCodes.NOT_FOUND).json({
                success: false,
                message: "Invalid or expired coupon code"
            });
        }

        return res.status(StatusCodes.OK).json({
            success: true,
            message: "Coupon applied successfully",
            data: promotion
        });
    } catch (error) {
        next(error);
    }
};

export const promotionController = {
    applyPromotion
};
