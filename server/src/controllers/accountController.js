import { StatusCodes } from 'http-status-codes'
import { accountService } from '../services/accountService.js'

const register = async (req, res, next) => {
    try {
        const createAccount = await accountService.register(req.body)
        res.status(StatusCodes.CREATED).json(createAccount)
    } catch (error) {
        next(error)
    }
}

const login = async (req, res, next) => {
    try {
        const account = await accountService.login(req.body)
        res.status(StatusCodes.OK).json(account)
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({
                success: false,
                message: error.message
            });
        }
        next(error)
    }
}

const getProfile = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const account = await accountService.getProfile(accountId)
        res.status(StatusCodes.OK).json(account)
    } catch (error) {
        next(error)
    }
}

const updateProfile = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const account = await accountService.updateProfile(accountId, req.body)
        res.status(StatusCodes.OK).json(account)
    } catch (error) {
        next(error)
    }
}

const changePassword = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const result = await accountService.changePassword(accountId, req.body)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({
                success: false,
                message: error.message
            });
        }
        next(error)
    }
}

const resetPassword = async (req, res, next) => {
    try {
        const result = await accountService.resetPassword(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        next(error);
    }
}

const googleLogin = async (req, res, next) => {
    try {
        const { idToken } = req.body
        const result = await accountService.googleLogin(idToken)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({
                success: false,
                message: error.message
            });
        }
        next(error)
    }
}

const sendOtp = async (req, res, next) => {
    try {
        const result = await accountService.sendOtp(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        next(error);
    }
}

const verifyOtp = async (req, res, next) => {
    try {
        const result = await accountService.verifyOtp(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        next(error);
    }
}

const registerFcmToken = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const { fcmToken } = req.body
        const result = await accountService.registerFcmToken(accountId, fcmToken)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message })
        }
        next(error)
    }
}

const refreshToken = async (req, res, next) => {
    try {
        const result = await accountService.refreshToken(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        next(error);
    }
}

export const accountController = {
    register,
    login,
    googleLogin,
    getProfile,
    updateProfile,
    changePassword,
    resetPassword,
    sendOtp,
    verifyOtp,
    registerFcmToken,
    refreshToken
}
