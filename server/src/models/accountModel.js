import mongoose from "mongoose";

const accountSchema = new mongoose.Schema({
    // 1. Account & security information group
    username: {
        type: String,
        required: true,
        unique: true,
        trim: true
    },
    email: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        lowercase: true
    },
    passwordHash: {
        type: String,
        required: true
    },

    refreshToken: {
        type: String
    },

    // 2. Personal information group has been moved to members collection

    // 3. System management group
    roleId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Role',
        required: true
    },
    status: {
        type: Number,
        default: 1
    },
    registerDate: {
        type: Date,
        default: Date.now
    }
}, {
    versionKey: false,
    collection: 'accounts',
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

accountSchema.virtual('memberInfo', {
    ref: 'Member',
    localField: '_id',
    foreignField: 'accountId',
    justOne: true
});

export const Account = mongoose.model('Account', accountSchema);