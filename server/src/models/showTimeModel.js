import mongoose from 'mongoose';

const showtimeSchema = new mongoose.Schema({
    movieId: {type: mongoose.Schema.Types.ObjectId, ref: "Movie",required: true},
    cinemaId: {type: mongoose.Schema.Types.ObjectId, ref: "Cinema", required: true},
    roomId: {type: mongoose.Schema.Types.ObjectId, ref: "CinemaRoom", required: true},
    showDate: {type: Date, required: true},
    showTime: {type: Date, required: true},
    startTime: {type: Date, required: true},
    endTime: {type: Date, required: true}
}, { timestamps: true, versionKey: false, collection: 'showtimes' });

export const Schedule = mongoose.model('Showtime', scheduleSchema);