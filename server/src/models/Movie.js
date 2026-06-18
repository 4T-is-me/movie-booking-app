import mongoose from 'mongoose';

const movieSchema = new mongoose.Schema({
    MOVIE_ID: { type: String, required: true, unique: true },
    ACTOR: { type: String, required: true },
    CINEMA_ROOM_ID: { type: Number, ref: 'CINEMA_ROOM', required: true },
    CONTENT: { type: String },
    DIRECTOR: { type: String, required: true },
    DURATION: { type: Number, required: true },
    FROM_DATE: { type: Date, required: true },
    MOVIE_PRODUCTION_COMPANY: { type: String, required: true },
    TO_DATE: { type: Date, required: true },
    VERSION: { type: String, required: true },
    MOVIE_NAME_ENGLISH: { type: String, required: true },
    MOVIE_NAME_VN: { type: String, required: true },
    LARGE_IMAGE: { type: String },
    SMALL_IMAGE: { type: String }
}, { versionKey: false, collection: 'MOVIE' });


export const movie = mongoose.model('MOVIE', movieSchema);