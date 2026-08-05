import express from 'express'
import { accountValidation } from '../validation/accountValidation.js'
import { accountController } from '../controllers/accountController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'
import multer from 'multer'
import path from 'path'
import fs from 'fs'
import { uploadMiddleware } from '../middlewares/uploadCloud.js'

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        const uploadDir = './uploads/'
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true })
        }
        cb(null, uploadDir)
    },
    filename: function (req, file, cb) {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9)
        cb(null, 'avatar-' + uniqueSuffix + path.extname(file.originalname))
    }
})

const upload = multer({ 
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
    fileFilter: (req, file, cb) => {
        if (file.mimetype.startsWith('image/')) {
            cb(null, true)
        } else {
            cb(new Error('Only image files are allowed!'), false)
        }
    }
})

const authRouter = express.Router()
authRouter.post('/register', accountValidation.register, accountController.register)
authRouter.post('/login', accountValidation.login, accountController.login)
authRouter.post('/google', accountController.googleLogin)
authRouter.post('/forgot-password/send-otp', accountController.sendOtp)
authRouter.post('/forgot-password/verify-otp', accountController.verifyOtp)
authRouter.post('/verify-otp', accountController.verifyOtp)
authRouter.post('/reset-password', accountValidation.resetPassword, accountController.resetPassword)
authRouter.post('/refresh-token', accountController.refreshToken)

const accountRouter = express.Router()
accountRouter.get('/profile', authMiddleware.isAuthorized, accountController.getProfile)
accountRouter.put('/profile', authMiddleware.isAuthorized, accountValidation.updateProfile, accountController.updateProfile)
accountRouter.put('/change-password', authMiddleware.isAuthorized, accountValidation.changePassword, accountController.changePassword)

// POST /api/accounts/fcm-token  – stores the Android/iOS FCM device token for push notification delivery
accountRouter.post('/fcm-token', authMiddleware.isAuthorized, accountController.registerFcmToken)

accountRouter.post('/upload-avatar', authMiddleware.isAuthorized, uploadMiddleware.upload.single('avatar'), uploadMiddleware.uploadToCloudinary('avatars', 'avatar'), (req, res, next) => {
    try {
        if (!req.body.avatar) {
            return res.status(400).json({ success: false, message: 'Please select an image file to upload.' })
        }
        res.status(200).json({
            success: true,
            avatarUrl: req.body.avatar
        })
    } catch (error) {
        next(error)
    }
})

export const accountRoute = {
    authRouter,
    accountRouter
}