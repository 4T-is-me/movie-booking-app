import mongoose from 'mongoose';

const promotionSchema = new mongoose.Schema({
    title: {type: String, required: true},
    detail:{type: String, required: true},
    discountLevel: {type: Number, required: true},
    image: {type: String},
    startTime: {type: Date, required: true},
    endTime: {type: Date, required: true}
}, { timestamps: true, versionKey: false, collection: 'promotions' });

export const promotion = mongoose.model('Promotions', promotionSchema);
