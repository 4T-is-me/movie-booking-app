import mongoose from 'mongoose';

const seatSchema = new mongoose.Schema({
    roomId: { type: mongoose.Schema.Types.ObjectId,  ref: "CinemaRoom",required: true },
    seatName:{type: String, required: true},
    price:{type: Number, required: true},
    row: {type: String, required: true},
    col: {type: Number, required: true},
    status: {type: String, default: 'active'}, // 'active', 'inactive'
    seatType: { type: String ,default: 'Regular'}, // 'Regular', 'VIP', 'Couple'
    isDeleted: { type: Boolean, default: false }
}, { versionKey: false, collection: 'seats' });

// Không cho trùng ghế trong cùng 1 phòng
seatSchema.index({roomId: 1, seatName: 1}, {unique: true});
seatSchema.index({roomId: 1, row: 1, col: 1});
export const Seat = mongoose.model('Seat', seatSchema);
