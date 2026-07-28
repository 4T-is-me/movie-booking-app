import mongoose from 'mongoose';

const payrollConfigSchema = new mongoose.Schema(
  {
    shiftRates: {
      type: Map,
      of: { type: Number, min: 0 },
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
      min: 1,
    },
    managerRevenueBonus: {
      type: Number,
      default: 2000000,
      min: 0,
    },
    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Account',
    },
  },
  {
    timestamps: true,
    versionKey: false,
    collection: 'payrollconfigs',
  }
);

export const PayrollConfig = mongoose.model('PayrollConfig', payrollConfigSchema);
