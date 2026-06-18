import mongoose from 'mongoose';

const showDatesSchema = new mongoose.Schema({
    SHOW_DATE_ID: { type: Number, required: true, unique: true },
    SHOW_DATE: { type: Date, required: true },
    DATE_NAME: { type: String }
}, { versionKey: false, collection: 'SHOW_DATES' });

export const showDates = mongoose.model('SHOW_DATES', showDatesSchema);