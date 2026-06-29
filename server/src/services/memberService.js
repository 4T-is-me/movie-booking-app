import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'
import { ScoreHistory } from '../models/scoreHistoryModel.js'
import { Member } from '../models/memberModel.js'

const getScoreHistory = async (accountId, page = 1, limit = 10) => {
    const member = await Member.findOne({ accountId }).lean()
    if (!member) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Member not found')
    }

    const skip = (page - 1) * limit
    const total = await ScoreHistory.countDocuments({ memberId: member._id })
    const histories = await ScoreHistory.find({ memberId: member._id })
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .lean()

    const formattedHistories = histories.map(history => {
        let type = 'earned';
        let amount = history.earnedPoints;

        if (history.usedPoints > 0) {
            type = 'used';
            amount = history.usedPoints;
        }

        return {
            type,
            amount,
            invoiceId: history.bookingId,
            createdAt: history.createdAt
        }
    })

    return {
        data: formattedHistories,
        pagination: {
            total,
            page,
            limit,
            totalPages: Math.ceil(total / limit)
        }
    }
}

export const memberService = {
    getScoreHistory
}
