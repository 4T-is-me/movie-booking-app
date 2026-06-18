import mongoose from 'mongoose';

const movieTypeSchema = new mongoose.Schema({
    MOVIE_ID: { type: String, ref: 'MOVIE', required: true },
    TYPE_ID: { type: Number, ref: 'TYPE', required: true }
    }, { versionKey: false, collection: 'MOVIE_TYPE' }
);

export const MovieType = mongoose.model('MOVIE_TYPE', movieTypeSchema);