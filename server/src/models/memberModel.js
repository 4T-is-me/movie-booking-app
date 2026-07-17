import mongoose from 'mongoose';

const memberSchema = new mongoose.Schema({
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: "Account", unique: true, required: true },
    fullName: { type: String, required: true },
    gender: { type: String },
    dateOfBirth: { type: Date },
    phoneNumber: { type: String, unique: true, sparse: true },
    identityCard: { type: String, unique: true, sparse: true },
    address: { type: String },
    image: { type: String },
    rewardPoints: { type: Number, default: 0 },
    membershipRank: { type: String, default: 'bronze' }, // bronze, silver, gold
    totalSpent: { type: Number, default: 0 },
    favoriteCinema: { type: String },
    favoriteGenres: { type: [String] }
}, { versionKey: false, collection: 'members' }
);

export const Member = mongoose.model('Member', memberSchema);