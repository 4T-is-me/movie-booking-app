import mongoose from 'mongoose';

const memberSchema = new mongoose.Schema({
    MEMBER_ID: { type: String, required: true, unique: true },
    SCORE: { type: Number, default: 0 },
    ACCOUNT_ID: { type: String, ref: 'ACCOUNT', required: true, unique: true }
    }, { versionKey: false, collection: 'MEMBER' }
);

export const member = mongoose.model('MEMBER', memberSchema);