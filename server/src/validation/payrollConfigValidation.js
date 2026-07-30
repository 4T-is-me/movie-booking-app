import Joi from 'joi';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';

/**
 * Validate update PayrollConfig request body.
 * At least one field must be provided.
 */
const updateConfig = async (req, res, next) => {
  const shiftRateSchema = Joi.object({
    Morning:   Joi.number().min(0).optional(),
    Afternoon: Joi.number().min(0).optional(),
    Evening:   Joi.number().min(0).optional(),
    Night:     Joi.number().min(0).optional(),
  });

  const correctCondition = Joi.object({
    shiftRates:          shiftRateSchema.optional(),
    holidayMultiplier:   Joi.number().min(1).max(10).optional(),
    managerRevenueBonus: Joi.number().min(0).optional(),
  }).or('shiftRates', 'holidayMultiplier', 'managerRevenueBonus');

  try {
    await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: false });
    next();
  } catch (error) {
    const errorMessage = error.details
      ? error.details.map((err) => err.message).join(', ')
      : error.message;
    next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
  }
};

export const payrollConfigValidation = {
  updateConfig,
};
