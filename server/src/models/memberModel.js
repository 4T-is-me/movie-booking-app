import mongoose from 'mongoose';

const memberSchema = new mongoose.Schema({
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: "Account", unique: true, required: true },
    fullName: { type: String, required: true },
    gender: { type: String, required: true },
    dateOfBirth: { type: Date, required: true },
    phoneNumber: { type: String, unique: true, required: true },
    identityCard: { type: String, required: true, unique: true },
    address: { type: String },
    image: { type: String },
    rewardPoints: { type: Number, default: 0 },
    membershipRank: { type: String, default: 'bronze' }, // bronze, silver, gold
    totalSpent: { type: Number, default: 0 }
}, { versionKey: false, collection: 'members' }
);

export const Member = mongoose.model('Member', memberSchema);