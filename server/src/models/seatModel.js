import mongoose from 'mongoose';

const seatSchema = new mongoose.Schema({
    roomId: { type: mongoose.Schema.Types.ObjectId,  ref: "CinemaRoom",required: true },
    seatName:{type: String, required: true},
    price:{type: Number, required: true},
    row: {type: String, required: true},
    col: {type: String, required: true},
    status: {type: String, default: 1, default: 'available'}, // 'available', 'hold', 'booked', 'unavailable'
    seatType: { type: String ,default: 1, default: 'Regular'}, // 'Regular', 'VIP', 'Couple'
}, { versionKey: false, collection: 'seats' });

// Không cho trùng ghế trong cùng 1 phòng
seatSchema.index({roomId: 1, seatName: 1}, {unique: true});
export const Seat = mongoose.model('Seat', seatSchema);
