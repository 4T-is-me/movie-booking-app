import mongoose from 'mongoose';

const showDateSchema = new mongoose.Schema({
    showDate: { type: Date, required: true, unique: true },
    isDeleted: { type: Boolean, default: false },
    deletedAt: { type: Date, default: null }
}, { timestamps: true, versionKey: false, collection: 'showdates' });

export const ShowDate = mongoose.model('ShowDate', showDateSchema);
