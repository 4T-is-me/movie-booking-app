import express from 'express'
import { accountValidation } from '../validation/accountValidation.js'
import { accountController } from '../controllers/accountController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'

const authRouter = express.Router()
authRouter.post('/register', accountValidation.register, accountController.register)
authRouter.post('/login', accountValidation.login, accountController.login)

const accountRouter = express.Router()
accountRouter.get('/profile', authMiddleware.isAuthorized, accountController.getProfile)
accountRouter.put('/profile', authMiddleware.isAuthorized, accountValidation.updateProfile, accountController.updateProfile)

export const accountRoute = {
    authRouter,
    accountRouter
}