import mongoose from 'mongoose';

const promotionUsageSchema = new mongoose.Schema({
    promotionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Promotions', required: true },
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true },
    invoiceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Booking', required: true },
    discountAmount: { type: Number, required: true },
    usedAt: { type: Date, default: Date.now }
}, { timestamps: false, versionKey: false, collection: 'promotion_usages' });

export const PromotionUsage = mongoose.model('PromotionUsage', promotionUsageSchema);
