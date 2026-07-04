import mongoose from "mongoose";

const accountSchema = new mongoose.Schema({
    // 1. Nhóm thông tin tài khoản & bảo mật (Đầu ảnh)
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
        // Make it optional for Google Login
    },
    authProvider: {
        type: String,
        enum: ['local', 'google'],
        default: 'local'
    },
    googleId: {
        type: String,
        unique: true,
        sparse: true // Allows multiple null values
    },

    refreshToken: {
        type: String
    },

    // 2. Nhóm thông tin cá nhân đã được chuyển sang bảng members

    // 3. Nhóm quản lý hệ thống (Cuối ảnh)
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