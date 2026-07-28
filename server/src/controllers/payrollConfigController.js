import { StatusCodes } from 'http-status-codes';
import { payrollConfigService } from '../services/payrollConfigService.js';

/**
 * GET /api/admin/payroll-config
 * Accessible by Admin and Manager.
 */
const getConfig = async (req, res, next) => {
  try {
    const config = await payrollConfigService.getConfig();
    return res.status(StatusCodes.OK).json({
      success: true,
      data: config,
    });
  } catch (error) {
    next(error);
  }
};

/**
 * PUT /api/admin/payroll-config
 * Admin only. Updates PayrollConfig and records who made the change.
 */
const updateConfig = async (req, res, next) => {
  try {
    const updatedBy = req.user.accountId;
    const config = await payrollConfigService.updateConfig(req.body, updatedBy);
    return res.status(StatusCodes.OK).json({
      success: true,
      message: 'Payroll configuration updated successfully.',
      data: config,
    });
  } catch (error) {
    next(error);
  }
};

export const payrollConfigController = {
  getConfig,
  updateConfig,
};
