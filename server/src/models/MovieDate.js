import mongoose from 'mongoose';

const movieDateSchema = new mongoose.Schema({
    MOVIE_ID: { type: String, ref: 'MOVIE', required: true },
    SHOW_DATE_ID: { type: Number, ref: 'SHOW_DATES', required: true }
}, { versionKey: false, collection: 'MOVIE_DATE' }
);
export const MovieDate = mongoose.model('MOVIE_DATE', movieDateSchema);