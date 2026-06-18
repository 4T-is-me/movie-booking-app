import mongoose from 'mongoose';

const movieScheduleSchema = new mongoose.Schema({
    SCHEDULE_ID: { type: Number, ref: 'SCHEDULE', required: true },
    MOVIE_ID: { type: String, ref: 'MOVIE', required: true }
    }, { versionKey: false, collection: 'MOVIE_SCHEDULE' }
);
export const movieSchedule = mongoose.model('MOVIE_SCHEDULE', movieScheduleSchema);