import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'
import { ScoreHistory } from '../models/scoreHistoryModel.js'
import { Member } from '../models/memberModel.js'
import mongoose from 'mongoose'
const getScoreHistory = async (accountId, page = 1, limit = 10) => {
    const member = await Member.findOne({ accountId }).lean()
    if (!member) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Member not found')
    }
    console.log(member)
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



const getMemberByIdentity = async (identity) => {
    let query = {};
    if (mongoose.Types.ObjectId.isValid(identity)) {
        query = { $or: [{ _id: identity }, { phoneNumber: identity }, { identityCard: identity }] };
    } else {
        query = { $or: [{ phoneNumber: identity }, { identityCard: identity }] };
    }
    const member = await Member.findOne(query).lean();
    if (!member) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Member not found');
    }
    return {
        success: true,
        data: {
            memberId: member._id,
            memberName: member.fullName,
            phoneNumber: member.phoneNumber,
            currentScore: member.rewardPoints
        }
    };
}

export const memberService = {
    getScoreHistory,
    getMemberByIdentity
}
