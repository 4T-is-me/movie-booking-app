import mongoose from 'mongoose'; 

const scheduleSeatSchema = new mongoose.Schema({
    showtimeId: {type: mongoose.Schema.Types.ObjectId, ref: "Showtime",required: true},
    seatId: {type: mongoose.Schema.Types.ObjectId, ref: "Seat", required: true},
    accountId: {type: mongoose.Schema.Types.ObjectId, ref: "Account", required: true},
    status: {type: String, required: true, enum: ['holding', 'booked']}, // 'holding' or 'booked'
    holdExpiresAt: {type: Date, default: null }
}, { versionKey: false, collection: 'schedule_seats' });

// Add TTL index to automatically delete expired "holding" seats (if holdExpiresAt passes)
scheduleSeatSchema.index({ holdExpiresAt: 1 }, { expireAfterSeconds: 0 });
// Add compound unique index so we don't have duplicate seat holdings
scheduleSeatSchema.index({ showtimeId: 1, seatId: 1 }, { unique: true });

export const ScheduleSeat = mongoose.model('scheduleSeats', scheduleSeatSchema);