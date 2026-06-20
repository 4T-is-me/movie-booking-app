import mongoose from 'mongoose';

const scheduleSchema = new mongoose.Schema({
    SCHEDULE_ID: { type: Number, required: true, unique: true },
    SCHEDULE_TIME: { type: String, required: true },
}, { versionKey: false, collection: 'SCHEDULE' });

export const Schedule = mongoose.model('SCHEDULE', scheduleSchema);