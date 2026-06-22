import mongoose from 'mongoose';

const seatSchema = new mongoose.Schema({
    SEAT_ID: { type: Number, required: true, unique: true },
    CINEMA_ROOM_ID: { type: Number, ref: 'CINEMA_ROOM', required: true },
    SEAT_COLUMN: { type: String, required: true },
    SEAT_ROW: { type: Number, required: true },
    SEAT_STATUS: { type: Number, required: true },
    SEAT_TYPE: { type: Number, required: true }
}, { versionKey: false, collection: 'SEAT' });

export const Seat = mongoose.model('SEAT', seatSchema);