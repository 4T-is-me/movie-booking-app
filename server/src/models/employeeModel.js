import mongoose from 'mongoose';

const employeeSchema = new mongoose.Schema({
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true, unique: true },
    cinemaId: { type: mongoose.Schema.Types.ObjectId, ref: 'Cinema', required: true},
    fullName: {type: String, required: true},
    position: {type: String, required: true},
    phone: {type: String, required: true},
    identityCard: {type: String, required: true, unique: true},
    salary: {type: Number, required: true},
    kpiPoints: {type: Number, default: 0},
    ticketSold: {type: Number, default: 0},
    rating: {type: Number, default: 0},
    totalRatings: {type: Number, default: 0},
    totalStars: {type: Number, default: 0},
    startData: {type: Date,  required: true}
}, { versionKey: false, collection: 'employees' });

export const employee = mongoose.model('Employee', employeeSchema);