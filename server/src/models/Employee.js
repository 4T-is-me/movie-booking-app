import mongoose from 'mongoose';

const employeeSchema = new mongoose.Schema({
    EMPLOYEE_ID: { type: String, required: true, unique: true },
    ACCOUNT_ID: { type: String, ref: 'ACCOUNT', required: true, unique: true }
}, { versionKey: false, collection: 'EMPLOYEE' });

export const Employee = mongoose.model('EMPLOYEE', employeeSchema);