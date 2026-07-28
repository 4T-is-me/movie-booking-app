import { PayrollConfig } from '../models/PayrollConfigModel.js';

/**
 * Get the singleton PayrollConfig document.
 * If none exists, create one with default values.
 */
const getConfig = async () => {
  let config = await PayrollConfig.findOne();
  if (!config) {
    config = await PayrollConfig.create({});
  }
  return config;
};

/**
 * Update PayrollConfig fields.
 * Automatically updates updatedAt via Mongoose timestamps.
 * @param {Object} data - fields to update (shiftRates, holidayMultiplier, managerRevenueBonus)
 * @param {String} updatedBy - accountId of the Admin performing the update
 */
const updateConfig = async (data, updatedBy) => {
  let config = await PayrollConfig.findOne();
  if (!config) {
    config = new PayrollConfig({});
  }

  const { shiftRates, holidayMultiplier, managerRevenueBonus } = data;

  if (shiftRates) {
    // Merge individual shift keys without overwriting unspecified ones
    const existing = config.shiftRates instanceof Map
      ? Object.fromEntries(config.shiftRates)
      : (config.shiftRates || {});

    const merged = { ...existing, ...shiftRates };
    config.shiftRates = merged;
  }

  if (holidayMultiplier !== undefined && holidayMultiplier !== null) {
    config.holidayMultiplier = holidayMultiplier;
  }

  if (managerRevenueBonus !== undefined && managerRevenueBonus !== null) {
    config.managerRevenueBonus = managerRevenueBonus;
  }

  if (updatedBy) {
    config.updatedBy = updatedBy;
  }

  await config.save();
  return config;
};

export const payrollConfigService = {
  getConfig,
  updateConfig,
};
