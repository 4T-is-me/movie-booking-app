import mongoose from 'mongoose';

const promotionSchema = new mongoose.Schema({
    PROMOTION_ID: { type: Number, required: true, unique: true },
    DETAIL: { type: String },
    DISCOUNT_LEVEL: { type: Number, required: true },
    END_TIME: { type: Date, required: true },
    IMAGE: { type: String },
    START_TIME: { type: Date, required: true },
    TITLE: { type: String, required: true }
}, { versionKey: false, collection: 'PROMOTION' });

export const promotion = mongoose.model('PROMOTION', promotionSchema);
