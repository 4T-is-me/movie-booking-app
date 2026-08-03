
const validateAge = (value, helpers) => {
    if (!value) return value;
    const today = new Date();
    const dob = new Date(value);
    if (dob > today) {
        return helpers.message('Date of birth cannot be in the future');
    }
    let age = today.getFullYear() - dob.getFullYear();
    const monthDiff = today.getMonth() - dob.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
        age--;
    }
    if (age < 15) {
        return helpers.message('You must be at least 15 years old');
    }
    if (age > 80) {
        return helpers.message('Age cannot exceed 80 years');
    }
    return value;
}

const validatePassword = (value, helpers) => {
    if (!value) return value;
    
    if (value.length < 6) {
        return helpers.message('Password must be at least 6 characters');
    }
    if (!/[A-Z]/.test(value)) {
        return helpers.message('Password must contain at least one uppercase letter');
    }
    if (!/[a-z]/.test(value)) {
        return helpers.message('Password must contain at least one lowercase letter');
    }
    if (!/[0-9]/.test(value)) {
        return helpers.message('Password must contain at least one number');
    }
    if (!/[!@#$%^&*()_+\-=\[\]{}|;:'",.<>?/]/.test(value)) {
        return helpers.message('Password must contain at least one special character');
    }
    
    return value;
}

const register = async (req, res, next) => {
    const correctCondition = Joi.object({
        username: Joi.string().required().min(3).max(30).pattern(/^[a-zA-Z0-9_]+$/).trim().strict().messages({
            'string.pattern.base': 'Username can only contain letters, numbers, and underscores (_)',
            'string.min': 'Username must be at least 3 characters',
            'string.max': 'Username cannot exceed 30 characters',
            'any.required': 'Username is required'
        }),
        email: Joi.string().required().pattern(/^[a-zA-Z0-9._%+-]+@gmail\.com$/).trim().strict().messages({
            'string.pattern.base': 'Email must be a valid address ending with @gmail.com',
            'any.required': 'Email is required'
        }),
        password: Joi.string().required().custom(validatePassword).messages({
            'any.required': 'Password is required'
        }),
        fullName: Joi.string().required().pattern(/^[^\d]+$/).trim().strict().messages({
            'string.pattern.base': 'Full name cannot contain numbers',
            'any.required': 'Full name is required'
        }),
        gender: Joi.string().required().trim().strict().messages({
            'any.required': 'Gender is required'
        }),
        dateOfBirth: Joi.date().required().custom(validateAge).messages({
            'any.required': 'Date of birth is required',
            'date.base': 'Invalid date format'
        }),
        phoneNumber: Joi.string().pattern(/^0\d{9}$/).required().trim().strict().messages({
            'string.pattern.base': 'Phone number must be 10 digits starting with 0',
            'any.required': 'Phone number is required'
        }),
        identityCard: Joi.string().required().pattern(/^[0-9]{9}$|^[0-9]{12}$/).trim().strict().messages({
            'string.pattern.base': 'Identity card must be 9 or 12 digits',
            'any.required': 'Identity card is required'
        }),
        address: Joi.string().trim().strict().allow('', null),
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
        email: Joi.string().required().trim().strict().messages({
            'any.required': 'Username, email or phone is required'
        }),
        password: Joi.string().required().min(6).strict().messages({
            'string.min': 'Password must be at least 6 characters',
            'any.required': 'Password is required'
        })
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
        email: Joi.string().pattern(/^[a-zA-Z0-9._%+-]+@gmail\.com$/).trim().strict().messages({
            'string.pattern.base': 'Email must end with @gmail.com'
        }),
        phone: Joi.string().pattern(/^0\d{9}$/).trim().strict().messages({
            'string.pattern.base': 'Phone number must be 10 digits starting with 0'
        }),
        address: Joi.string().trim().strict().allow('', null),
        fullName: Joi.string().pattern(/^[^\d]+$/).trim().strict().messages({
            'string.pattern.base': 'Full name cannot contain numbers'
        }),
        dob: Joi.date().custom(validateAge).allow('', null).messages({
            'date.base': 'Invalid date format'
        }),
        gender: Joi.string().trim().strict().allow('', null),
        identityCard: Joi.string().pattern(/^[0-9]{9}$|^[0-9]{12}$/).trim().strict().messages({
            'string.pattern.base': 'Identity card must be 9 or 12 digits'
        }),
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
        currentPassword: Joi.string().required().strict().messages({
            'any.required': 'Current password is required'
        }),
        newPassword: Joi.string().required().custom(validatePassword).messages({
            'any.required': 'New password is required'
        })
    })

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false })
        next()
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
}

const resetPassword = async (req, res, next) => {
    const correctCondition = Joi.object({
        email: Joi.string().required().trim().strict().messages({
            'any.required': 'Email is required'
        }),
        newPassword: Joi.string().required().custom(validatePassword).messages({
            'any.required': 'New password is required'
        })
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
    changePassword,
    resetPassword
}

