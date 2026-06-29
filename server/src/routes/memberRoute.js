import express from 'express'
import { memberController } from '../controllers/memberController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'

const memberRouter = express.Router()

memberRouter.get('/score-history', authMiddleware.isAuthorized, memberController.getScoreHistory)

export const memberRoute = {
    memberRouter
}
