import { Promotion } from "../models/promotionModel.js";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from "http-status-codes";

const validateAndApplyPromotion = async (couponCode, accountId, orderTotal, session = null) => {
    const promotion = await Promotion.findOne({ 
        couponCode, 
        isDeleted: false 
    }).session(session);

    if (!promotion) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Mã giảm giá không tồn tại hoặc đã bị xóa");
    }

    const now = new Date();
    if (now < promotion.startDate || now > promotion.endDate) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Mã giảm giá không nằm trong thời gian áp dụng");
    }

    if (promotion.maxUsage !== -1 && promotion.usageCount >= promotion.maxUsage) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng");
    }

    if (orderTotal < promotion.minOrderValue) {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá. Yêu cầu: ${promotion.minOrderValue}`);
    }

    let discountAmount = 0;
    if (promotion.discountType === 'percentage') {
        discountAmount = Math.floor(orderTotal * (promotion.discountValue / 100));
    } else if (promotion.discountType === 'fixed') {
        discountAmount = promotion.discountValue;
    }

    // Đảm bảo giảm không quá giá trị đơn hàng
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
