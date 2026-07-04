import Joi from 'joi'
import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'

const register = async (req, res, next) => {
    const maxDate = new Date();
    maxDate.setFullYear(maxDate.getFullYear() - 15);

    const correctCondition = Joi.object({
        username: Joi.string().required().min(3).max(30).trim().strict(),
        email: Joi.string().required().email().trim().strict(),
        password: Joi.string().required().min(6).strict(),
        confirmPassword: Joi.any().valid(Joi.ref('password')).required().messages({
            'any.only': '"confirmPassword" does not match'
        }),
        fullName: Joi.string().required().pattern(/^[^0-9]+$/).trim().strict().messages({
            'string.pattern.base': '"fullName" cannot contain numbers'
        }),
        gender: Joi.string().valid('Male', 'Female').required().trim().strict(),
        dateOfBirth: Joi.date().required().max(maxDate).messages({
            'date.max': '"dateOfBirth" must indicate an age greater than 15 years'
        }),
        phoneNumber: Joi.string().pattern(/^0\d{9}$/).required().trim().strict().messages({
            'string.pattern.base': '"phoneNumber" must be exactly 10 digits and start with 0'
        }),
        identityCard: Joi.string().required().length(12).pattern(/^[0-9]+$/).trim().strict().messages({
            'string.pattern.base': '"identityCard" must only contain digits',
            'string.length': '"identityCard" must be exactly 12 digits'
        }),
        address: Joi.string().required().trim().strict(),
        image: Joi.string().trim().strict().allow('', null)
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

export const accountValidation = {
    register,
    login,
    updateProfile
}
