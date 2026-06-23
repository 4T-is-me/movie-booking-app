import mongoose from 'mongoose'; 

const scheduleSeatSchema = new mongoose.Schema({
    showtimeId: {type: mongoose.Schema.Types.ObjectId, ref: "Showtime",required: true},
    seatId: {type: mongoose.Schema.Types.ObjectId, ref: "Seat", required: true},
    accountId: {type: mongoose.Schema.Types.ObjectId, ref: "Account", default: null}, // default: null => status = AVAILABLE
    status: {type: Number, required: true}, //  AVAILABLE = 0, HOLD = 1, BOOKED = 2, UNAVAILABLE = 3
    holdExpiresAt: {type: Date,default: null }
}, { versionKey: false, collection: 'schedule_seats' }
);
export const ScheduleSeat = mongoose.model('scheduleSeats', scheduleSeatSchema);