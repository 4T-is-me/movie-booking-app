import mongoose from 'mongoose';

const scheduleSeatSchema = new mongoose.Schema({
    SCHEDULE_SEAT_ID: { type: String, required: true, unique: true },
    MOVIE_ID: { type: String, ref: 'MOVIE', required: true },
    SCHEDULE_ID: { type: Number, ref: 'SCHEDULE', required: true },
    SEAT_ID: { type: Number, ref: 'SEAT', required: true },
    SEAT_COLUMN: { type: String, required: true },
    SEAT_ROW: { type: Number, required: true },
    SEAT_STATUS: { type: Number, required: true },
    SEAT_TYPE: { type: Number, required: true }
    }, { versionKey: false, collection: 'SCHEDULE_SEAT' }
);
export const ScheduleSeat = mongoose.model('SCHEDULE_SEAT', scheduleSeatSchema);