import mongoose from "mongoose";

const accountSchema = new mongoose.Schema({
    ACCOUNT_ID: {type: String, required: true, unique: true},
    ADDRESS: {type: String},
    DATE_OF_BIRTH: { type: Date },
    EMAIL: { type: String, required: true, unique: true },
    FULL_NAME: { type: String, required: true },
    GENDER: { type: String },
    IDENTITY_CARD: { type: String, required: true, unique: true },
    IMAGE: { type: String },
    PASSWORD: { type: String, required: true },
    PHONE_NUMBER: { type: String, required: true },
    USERNAME: {type: String, required: true, unique: true},
    REGISTER_DATE: { type: Date, default: Date.now },
    ROLE_ID: { type: Number, ref: 'ROLES', required: true },
    STATUS: { type: Number, default: 1 }
}, { versionKey: false, collection: 'ACCOUNT' });
//tắt tính năng tự động thêm trường vào document
// collection: 'ACCOUNT' để chỉ định tên collection trong MongoDB là ACCOUNT 

export const account = mongoose.model('ACCOUNT', accountSchema);
