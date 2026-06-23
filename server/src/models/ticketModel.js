import mongoose from 'mongoose';

const ticketSchema = new mongoose.Schema({
    TICKET_ID: { type: Number, required: true, unique: true },
    PRICE: { type: Number, required: true },
    TICKET_TYPE: { type: Number, required: true }
}, { versionKey: false, collection: 'TICKET' });

export const ticket = mongoose.model('TICKET', ticketSchema);