import { Promotion } from "../models/promotionModel.js";
import { promotionService } from "../services/promotionService.js";
import { StatusCodes } from "http-status-codes";

// Keep existing applyPromotion for backward compatibility if needed, but we focus on validatePromotion
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

const validatePromotion = async (req, res, next) => {
    try {
        const { couponCode, orderTotal } = req.body;
        const accountId = req.user?.accountId;
        
        if (!couponCode || orderTotal === undefined) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "couponCode and orderTotal are required"
            });
        }
        
        const result = await promotionService.validateAndApplyPromotion(couponCode, accountId, orderTotal);
        
        return res.status(StatusCodes.OK).json({
            success: true,
            data: {
                isValid: result.isValid,
                discountAmount: result.discountAmount,
                finalPrice: orderTotal - result.discountAmount
            }
        });
    } catch (error) {
        next(error);
    }
};

const getPromotions = async (req, res, next) => {
    try {
        const now = new Date();
        const promotions = await Promotion.find({
            isDeleted: false,
            endDate: { $gte: now }
        }).sort({ createdAt: -1 });

        return res.status(StatusCodes.OK).json({
            success: true,
            data: promotions
        });
    } catch (error) {
        next(error);
    }
};

export const promotionController = {
    applyPromotion,
    validatePromotion,
    getPromotions
};
