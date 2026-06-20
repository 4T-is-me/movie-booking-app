import mongoose from 'mongoose';

const cinemaRoomSchema = new mongoose.Schema({
    CINEMA_ROOM_ID: { type: Number, required: true, unique: true },
    CINEMA_ROOM_NAME: { type: String, required: true },
    SEAT_CAPACITY: { type: Number, required: true }
}, { versionKey: false, collection: 'CINEMA_ROOM' });

export const CinemaRoom = mongoose.model('CINEMA_ROOM', cinemaRoomSchema);