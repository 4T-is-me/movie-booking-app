import { Promotion } from '../models/promotionModel.js';

export const getPromotions = async (query = {}) => {
    // Only return non-deleted promotions unless specified otherwise
    const filter = { isDeleted: false, ...query };
    return await Promotion.find(filter).sort({ createdAt: -1 });
};

export const createPromotion = async (promoData) => {
    const promotion = new Promotion(promoData);
    return await promotion.save();
};

export const updatePromotion = async (promoId, updateData) => {
    return await Promotion.findByIdAndUpdate(promoId, updateData, { new: true });
};

export const deletePromotion = async (promoId) => {
    // Soft delete: set isDeleted to true
    return await Promotion.findByIdAndUpdate(promoId, { isDeleted: true }, { new: true });
};
