import Joi from 'joi'
import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'

const register = async (req, res, next) => {
    const correctCondition = Joi.object({
        username: Joi.string().required().min(3).max(30).trim().strict(),
        email: Joi.string().required().email().trim().strict(),
        password: Joi.string().required().min(6).strict(),
        fullName: Joi.string().required().trim().strict(),
        gender: Joi.string().required().trim().strict(),
        dateOfBirth: Joi.date().required(),
        phoneNumber: Joi.string().pattern(/^0\d{9}$/).required().trim().strict(),
        identityCard: Joi.string().required().length(12).pattern(/^[0-9]+$/).trim().strict(),
        address: Joi.string().trim().strict(),
        image: Joi.string().trim().strict()
    })

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: true })
        next()
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
}

const login = async (req, res, next) => {
    const correctCondition = Joi.object({
        email: Joi.string().required().trim().strict(),
        password: Joi.string().required().min(6).strict()
    })

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false })
        next()
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
}

const updateProfile = async (req, res, next) => {
    const correctCondition = Joi.object({
        email: Joi.string().email().trim().strict(),
        phone: Joi.string().pattern(/^0\d{9}$/).trim().strict(),
        address: Joi.string().trim().strict().allow('', null),
        fullName: Joi.string().trim().strict(),
        dob: Joi.date().allow('', null),
        gender: Joi.string().trim().strict().allow('', null),
        identityCard: Joi.string().length(12).pattern(/^[0-9]+$/).trim().strict(),
        favoriteCinema: Joi.string().trim().strict().allow('', null),
        favoriteGenres: Joi.array().items(Joi.string().trim().strict()).allow(null),
        avatar: Joi.string().trim().strict().allow('', null)
    })

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: false })
        next()
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
}

const changePassword = async (req, res, next) => {
    const correctCondition = Joi.object({
        currentPassword: Joi.string().required().strict(),
        newPassword: Joi.string().required().min(6).strict()
    })

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false })
        next()
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
}

export const accountValidation = {
    register,
    login,
    updateProfile,
    changePassword
}
