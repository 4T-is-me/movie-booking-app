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

const googleLogin = async (req, res, next) => {
    try {
        const result = await accountService.googleLogin(req.body.idToken)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        console.error("Google Login Error:", error);
        return res.status(error.statusCode || 500).json({
            success: false,
            message: error.message || 'Internal Server Error'
        });
    }
}

const sendOtp = async (req, res, next) => {
    try {
        const result = await accountService.sendOtp(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        next(error);
    }
}

const verifyOtp = async (req, res, next) => {
    try {
        const result = await accountService.verifyOtp(req.body);
        res.status(StatusCodes.OK).json(result);
    } catch (error) {
        next(error);
    }
}

export const accountController = {
    register,
    login,
    googleLogin,
    getProfile,
    updateProfile,
    sendOtp,
    verifyOtp
}
