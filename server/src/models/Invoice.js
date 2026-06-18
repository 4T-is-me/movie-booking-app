import mongoose from 'mongoose';

const invoiceSchema = new mongoose.Schema({
    INVOICE_ID: { type: String, required: true, unique: true },
    ACCOUNT_ID: { type: String, ref: 'ACCOUNT', required: true },
    ADD_SCORE: { type: Number, default: 0 },
    BOOKING_DATE: { type: Date, default: Date.now },
    MOVIE_NAME: { type: String, required: true },
    SCHEDULE_SHOW: { type: Date, required: true },
    SCHEDULE_SHOW_TIME: { type: String, required: true },
    STATUS: { type: Number, default: 1 },
    TOTAL_MONEY: { type: Number, required: true },
    USE_SCORE: { type: Number, default: 0 },
    SEAT: { type: String, required: true }
}, { versionKey: false, collection: 'INVOICE' });

export const invoice = mongoose.model('INVOICE', invoiceSchema);