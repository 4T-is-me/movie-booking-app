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
        required: true
    },

    refreshToken: {
        type: String
    },

    // 2. Nhóm thông tin cá nhân (Giữa ảnh)
    fullName: {
        type: String,
        required: true,
        trim: true
    },
    gender: {
        type: String
    },
    dateOfBirth: {
        type: Date
    },
    phoneNumber: {
        type: String,
        required: true,
        unique: true,
        trim: true
    },
    identityCard: {
        type: String,
        required: true,
        unique: true,
        trim: true
    },
    address: {
        type: String,
        trim: true
    },
    image: {
        type: String
    },

    // 3. Nhóm quản lý hệ thống (Cuối ảnh)
    roleId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'roles',
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
    collection: 'accounts'
});

export const Account = mongoose.model('ACCOUNT', accountSchema);