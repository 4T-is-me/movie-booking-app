import { Promotion } from '../models/promotionModel.js';

export const createPromotion = async (promoData) => {
    const promotion = new Promotion(promoData);
    return await promotion.save();
};

export const updatePromotion = async (promoId, updateData) => {
    return await Promotion.findByIdAndUpdate(promoId, updateData, { new: true });
};

export const deletePromotion = async (promoId) => {
    return await Promotion.findByIdAndDelete(promoId);
};
