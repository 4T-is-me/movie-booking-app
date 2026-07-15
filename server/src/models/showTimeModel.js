import mongoose from 'mongoose';

const showtimeSchema = new mongoose.Schema({
    movieId: {type: mongoose.Schema.Types.ObjectId, ref: "Movie",required: true},
    cinemaId: {type: mongoose.Schema.Types.ObjectId, ref: "Cinema", required: true},
    roomId: {type: mongoose.Schema.Types.ObjectId, ref: "CinemaRoom"},
    showDate: {type: Date},
    showTime: {type: String},
    startTime: {type: Date},
    endTime: {type: Date},
    ticketPrice: {type: Number},
    isDeleted: {type: Boolean, default: false},
    deletedAt: {type: Date, default: null},
    status: {type: String, default: 'published'}
}, { timestamps: true, versionKey: false, collection: 'showtimes' });

showtimeSchema.index({ movieId: 1, showDate: 1 });
showtimeSchema.index({ cinemaId: 1, showDate: 1, startTime: 1 });

export const Showtime = mongoose.model('Showtime', showtimeSchema);