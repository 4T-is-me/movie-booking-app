import mongoose from 'mongoose';

const promotionSchema = new mongoose.Schema({
    title: { type: String, required: true },
    detail: { type: String, required: true },
    discountLevel: { type: Number },
    image: { type: String },
    startTime: { type: Date },
    endTime: { type: Date },

    // New fields according to Issue 34 requirements
    couponCode: { type: String, required: true, unique: true },
    discountType: { type: String, enum: ['percentage', 'fixed'], required: true },
    discountValue: { type: Number, required: true },
    minOrderValue: { type: Number, default: 0 },
    maxUsage: { type: Number, default: -1 },
    usageCount: { type: Number, default: 0 },
    startDate: { type: Date, required: true },
    endDate: { type: Date, required: true },
    isDeleted: { type: Boolean, default: false }
}, { timestamps: true, versionKey: false, collection: 'promotions' });

// Add required indexes
promotionSchema.index({ startDate: 1, endDate: 1, isDeleted: 1 });

export const Promotion = mongoose.model('Promotions', promotionSchema);
