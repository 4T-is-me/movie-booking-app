import express from 'express'
import { memberController } from '../controllers/memberController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'

const memberRouter = express.Router()

memberRouter.get('/score-history', authMiddleware.isAuthorized, memberController.getScoreHistory)
memberRouter.get('/:identity', authMiddleware.isAuthorized, memberController.verifyMember)

export const memberRoute = {
    memberRouter
}
