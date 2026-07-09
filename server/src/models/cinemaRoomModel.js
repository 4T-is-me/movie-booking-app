import mongoose from 'mongoose';

const cinemaRoomSchema = new mongoose.Schema({
    cinemaId: { type: mongoose.Schema.Types.ObjectId, ref: 'Cinema', required: true },
    roomName: { type: String, required: true },
    roomType: { type: String, default: 'Standard' }, 
    screenType: { type: String, enum: ["2D", "3D", "IMAX"], default: '2D' },
    totalSeats: { type: Number, required: true },
    rowsCount: { type: Number, required: true },
    colsCount: { type: Number, required: true },
    status: { type: String, default: 'active' }, // 'active', 'inactive', 'maintenance'
    isDeleted: { type: Boolean, default: false }
}, { versionKey: false, collection: 'cinema_rooms' });

export const CinemaRoom = mongoose.model('CinemaRoom', cinemaRoomSchema);