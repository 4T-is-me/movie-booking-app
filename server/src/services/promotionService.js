import { Promotion } from "../models/promotionModel.js";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from "http-status-codes";

const validateAndApplyPromotion = async (couponCode, accountId, orderTotal, session = null) => {
    const promotion = await Promotion.findOne({ 
        couponCode, 
        isDeleted: false 
    }).session(session);

    if (!promotion) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Promotion code does not exist or has been deleted");
    }

    const now = new Date();
    if (now < promotion.startDate || now > promotion.endDate) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Promotion code is not within the validity period");
    }

    if (promotion.maxUsage !== -1 && promotion.usageCount >= promotion.maxUsage) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Promotion code usage limit reached");
    }

    if (orderTotal < promotion.minOrderValue) {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Order does not meet the minimum value requirement to apply this promotion code. Required: ${promotion.minOrderValue}`);
    }

    let discountAmount = 0;
    if (promotion.discountType === 'percentage') {
        discountAmount = Math.floor(orderTotal * (promotion.discountValue / 100));
    } else if (promotion.discountType === 'fixed') {
        discountAmount = promotion.discountValue;
    }

    // Ensure discount does not exceed order total
    if (discountAmount > orderTotal) {
        discountAmount = orderTotal;
    }

    return { 
        isValid: true, 
        discountAmount, 
        promotionId: promotion._id,
        promotion 
    };
};

export const promotionService = {
    validateAndApplyPromotion
};
