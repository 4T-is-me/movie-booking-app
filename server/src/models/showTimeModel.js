import mongoose from 'mongoose';

const showtimeSchema = new mongoose.Schema({
    movieId: {type: mongoose.Schema.Types.ObjectId, ref: "Movie",required: true},
    cinemaId: {type: mongoose.Schema.Types.ObjectId, ref: "Cinema", required: true},
    roomId: {type: mongoose.Schema.Types.ObjectId, ref: "CinemaRoom"},
    showDate: {type: Date},
    startTime: {type: Date},
    endTime: {type: Date}
}, { timestamps: true, versionKey: false, collection: 'showtimes' });

export const Showtime = mongoose.model('Showtime', showtimeSchema);