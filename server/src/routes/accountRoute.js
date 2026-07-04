import express from 'express'
import { accountValidation } from '../validation/accountValidation.js'
import { accountController } from '../controllers/accountController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'
import multer from 'multer'
import path from 'path'
import fs from 'fs'

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
authRouter.post('/check-register', accountValidation.register, accountController.checkRegister)
authRouter.post('/register', accountValidation.register, accountController.register)
authRouter.post('/login', accountValidation.login, accountController.login)
authRouter.post('/google-login', accountController.googleLogin)
authRouter.post('/send-otp', accountController.sendOtp)
authRouter.post('/verify-otp', accountController.verifyOtp)
const accountRouter = express.Router()
accountRouter.get('/profile', authMiddleware.isAuthorized, accountController.getProfile)
accountRouter.put('/profile', authMiddleware.isAuthorized, accountValidation.updateProfile, accountController.updateProfile)

accountRouter.post('/upload-avatar', authMiddleware.isAuthorized, upload.single('avatar'), (req, res, next) => {
    try {
        if (!req.file) {
            return res.status(400).json({ success: false, message: 'Please select an image file to upload.' })
        }
        const host = req.get('host')
        const protocol = req.protocol
        const avatarUrl = `${protocol}://${host}/uploads/${req.file.filename}`
        res.status(200).json({
            success: true,
            avatarUrl: avatarUrl
        })
    } catch (error) {
        next(error)
    }
})

export const accountRoute = {
    authRouter,
    accountRouter
}