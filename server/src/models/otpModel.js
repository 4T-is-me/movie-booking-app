import mongoose from "mongoose";

const otpSchema = new mongoose.Schema({
    email: { type: String, required: true },
    otp: { type: String, required: true },
    type: { type: String, default: 'forgot_password' },
    payload: { type: Object },
    createdAt: { type: Date, default: Date.now, expires: 300 } // Tự động xoá sau 300 giây (5 phút)
});

export const Otp = mongoose.model('Otp', otpSchema);