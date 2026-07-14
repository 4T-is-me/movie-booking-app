import { StatusCodes } from 'http-status-codes'
import { memberService } from '../services/memberService.js'

const getScoreHistory = async (req, res, next) => {
    try {
        const accountId = req.user.accountId
        const { page, limit } = req.query
        const result = await memberService.getScoreHistory(accountId, parseInt(page) || 1, parseInt(limit) || 10)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        next(error)
    }
}

const verifyMember = async (req, res, next) => {
    try {
        const { identity } = req.params
        const result = await memberService.getMemberByIdentity(identity)
        res.status(StatusCodes.OK).json(result)
    } catch (error) {
        next(error)
    }
}

export const memberController = {
    getScoreHistory,
    verifyMember
}
