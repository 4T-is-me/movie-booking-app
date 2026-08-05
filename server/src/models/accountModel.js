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

    tokenVersion: {
        type: Number,
        default: 0
    },

    // FCM token for push notification delivery (Android / iOS)
    fcmToken: {
        type: String,
        default: null
    },
    fcmTokens: {
        type: [String],
        default: []
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
    },
    isDeleted: {
        type: Boolean,
        default: false
    }
}, {
    versionKey: false,
    timestamps: true,
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

accountSchema.virtual('managerInfo', {
    ref: 'Manager',
    localField: '_id',
    foreignField: 'accountId',
    justOne: true
});

accountSchema.virtual('staffInfo', {
    ref: 'Staff',
    localField: '_id',
    foreignField: 'accountId',
    justOne: true
});

export const Account = mongoose.model('Account', accountSchema);