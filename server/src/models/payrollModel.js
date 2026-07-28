import mongoose from 'mongoose';

const payrollSchema = new mongoose.Schema(
  {
    employeeId: { type: mongoose.Schema.Types.ObjectId,required: true},
    cinemaId: {type: mongoose.Schema.Types.ObjectId, ref: 'Cinema',required: true},
    month: {type: Number, required: true,min: 1, max: 12},
    year: {type: Number,required: true},
    baseSalary: {type: Number, default: 0},
    shiftSalary: {type: Number, default: 0},
    bonus: { type: Number, default: 0},
    penalty: {type: Number, default: 0 },
    totalSalary: { type: Number, required: true },
    status: { type: String,enum: ['Draft', 'Paid'],default: 'Draft'},
    generatedAt: {type: Date,default: Date.now},
    paidAt: {type: Date,default: null},
    shiftIds: [{ type: mongoose.Schema.Types.ObjectId, ref: 'StaffShift' }],
  },
  {
    versionKey: false,
    collection: 'payrolls',
  }
);

// Tránh tạo trùng bảng lương cho cùng 1 nhân viên trong 1 tháng
payrollSchema.index({ employeeId: 1, month: 1, year: 1 }, { unique: true });

export const Payroll = mongoose.model('Payroll', payrollSchema);
