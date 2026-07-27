import mongoose from 'mongoose';

const payrollConfigSchema = new mongoose.Schema(
  {
    shiftRates: {
      type: Map,
      of: Number,
      default: {
        Morning: 150000,
        Afternoon: 150000,
        Evening: 150000,
        Night: 200000,
      },
    },
    holidayMultiplier: {
      type: Number,
      default: 1.5,
    },
    managerRevenueBonus: {
      type: Number,
      default: 2000000,
    },
    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Admin',
    },
  },
  {
    timestamps: true,
    versionKey: false,
    collection: 'payrollconfigs',
  }
);

export const PayrollConfig = mongoose.model('PayrollConfig', payrollConfigSchema);