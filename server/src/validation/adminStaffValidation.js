import Joi from 'joi';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';

const createStaff = async (req, res, next) => {
    const correctCondition = Joi.object({
        username: Joi.string().required().min(3).max(30).trim().strict(),
        email: Joi.string().required().email().trim().strict(),
        password: Joi.string().required().min(6).strict(),
        role: Joi.string().valid('Staff', 'Employee', 'Manager', 'staff', 'employee', 'manager').trim().strict(),
        roleId: Joi.string().pattern(/^[0-9a-fA-F]{24}$/).trim().strict(),
        fullName: Joi.string().required().trim().strict(),
        cinemaId: Joi.string().pattern(/^[0-9a-fA-F]{24}$/).required().trim().strict(),
        position: Joi.string().required().trim().strict(),
        phone: Joi.string().pattern(/^0\d{9}$/).required().trim().strict(),
        identityCard: Joi.string().required().length(12).pattern(/^[0-9]+$/).trim().strict(),
        salary: Joi.number().required().min(0),
        startData: Joi.date().required()
    }).oxor('role', 'roleId');

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: true });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

const updateStaff = async (req, res, next) => {
    const correctCondition = Joi.object({
        fullName: Joi.string().trim().strict(),
        email: Joi.string().email().trim().strict(),
        phone: Joi.string().pattern(/^0\d{9}$/).trim().strict(),
        address: Joi.string().trim().strict().allow('', null),
        cinemaId: Joi.string().pattern(/^[0-9a-fA-F]{24}$/).trim().strict(),
        position: Joi.string().trim().strict(),
        salary: Joi.number().min(0),
        startData: Joi.date()
    });

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: true });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

const updateStaffStatus = async (req, res, next) => {
    const correctCondition = Joi.object({
        status: Joi.number().valid(1, 2).required()
    });

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

const updateStaffRole = async (req, res, next) => {
    const correctCondition = Joi.object({
        role: Joi.string().valid('Staff', 'Employee', 'Manager', 'Admin', 'staff', 'employee', 'manager', 'admin').trim().strict(),
        roleId: Joi.string().pattern(/^[0-9a-fA-F]{24}$/).trim().strict()
    }).xor('role', 'roleId').messages({
        'object.xor': 'Phải cung cấp role (tên role) hoặc roleId (ID của role) và không được cung cấp cả hai.'
    });

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

export const adminStaffValidation = {
    createStaff,
    updateStaff,
    updateStaffStatus,
    updateStaffRole
};
