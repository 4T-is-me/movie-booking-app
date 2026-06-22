import mongoose from 'mongoose';

const typeSchema = new mongoose.Schema({
    TYPE_ID: { type: Number, required: true, unique: true },
    TYPE_NAME: { type: String, required: true }
}, { versionKey: false, collection: 'TYPE' });

export const type = mongoose.model('TYPE', typeSchema);