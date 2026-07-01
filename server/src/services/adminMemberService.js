import { Member } from '../models/memberModel.js';
import { Account } from '../models/accountModel.js';
import mongoose from 'mongoose';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';
import { ScoreHistory } from '../models/scoreHistoryModel.js';

/**
 * Lấy danh sách thành viên với các bộ lọc trạng thái và tìm kiếm
 * @param {Object} options 
 * @param {number} options.page
 * @param {number} options.limit
 * @param {number|string} options.status Trạng thái của tài khoản liên kết (1: Hoạt động, 0: Khóa, v.v.)
 * @param {string} options.search Từ khóa tìm kiếm (tên, số điện thoại, CMND, email, username)
 */
export const getMemberList = async ({ page = 1, limit = 10, status, search }) => {
    const skip = (page - 1) * limit;
    
    let accountFilter = {};
    let shouldFilterByAccount = false;
    
    if (status !== undefined && status !== null && status !== '') {
        accountFilter.status = Number(status);
        shouldFilterByAccount = true;
    }
    
    const memberFilter = {};
    
    if (shouldFilterByAccount) {
        if (search && search.trim() !== '') {
            const cleanSearch = search.trim();
            //get all account match with status
            const possibleAccounts = await Account.find(accountFilter).select('_id').lean();
            const possibleAccountIds = possibleAccounts.map(a => a._id);
            
            //get all account match with status and search
            const searchAccounts = await Account.find({
                ...accountFilter,
                $or: [
                    { email: { $regex: cleanSearch, $options: 'i' } },
                    { username: { $regex: cleanSearch, $options: 'i' } }
                ]
            }).select('_id').lean();
            const searchAccountIds = searchAccounts.map(a => a._id);
            
            //force  member account must be match with status 
            memberFilter.accountId = { $in: possibleAccountIds };
            
            // search member by name, phone, identity card or account match search
            memberFilter.$or = [
                { fullName: { $regex: cleanSearch, $options: 'i' } },
                { phoneNumber: { $regex: cleanSearch, $options: 'i' } },
                { identityCard: { $regex: cleanSearch, $options: 'i' } },
                { accountId: { $in: searchAccountIds } }
            ];
        } else {
            // Only filter by account status
            const matchingAccounts = await Account.find(accountFilter).select('_id').lean();
            const matchingAccountIds = matchingAccounts.map(a => a._id);
            memberFilter.accountId = { $in: matchingAccountIds };
        }
    } else {
        // No filter account status, only search by keyword
        if (search && search.trim() !== '') {
            const cleanSearch = search.trim();
            
            const searchAccounts = await Account.find({
                $or: [
                    { email: { $regex: cleanSearch, $options: 'i' } },
                    { username: { $regex: cleanSearch, $options: 'i' } }
                ]
            }).select('_id').lean();
            const searchAccountIds = searchAccounts.map(a => a._id);
            
            memberFilter.$or = [
                { fullName: { $regex: cleanSearch, $options: 'i' } },
                { phoneNumber: { $regex: cleanSearch, $options: 'i' } },
                { identityCard: { $regex: cleanSearch, $options: 'i' } },
                { accountId: { $in: searchAccountIds } }
            ];
        }
    }
    
    const total = await Member.countDocuments(memberFilter);
    const members = await Member.find(memberFilter)
        .populate({
            path: 'accountId',
            select: '-passwordHash -refreshToken'
        })
        .skip(skip)
        .limit(Number(limit))
        .lean();
        
    return {
        members,
        total,
        totalPages: Math.ceil(total / limit),
        currentPage: page
    };
};

/**
 * edit score member
 * @param {string} memberId 
 * @param {Object} data 
 * @param {number} data.adjustment add or deduct points
 * @param {string} data.reason reason for adjusting points
 */
export const adjustMemberScore = async (memberId, { adjustment, reason }) => {
    const session = await mongoose.startSession();
    try {
        session.startTransaction();

        // 1. find member
        const member = await Member.findById(memberId).session(session);
        if (!member) {
            throw new ApiError(StatusCodes.NOT_FOUND, 'Không tìm thấy thành viên.');
        }

        // 2. Check if deduction
        if (adjustment < 0 && member.rewardPoints + adjustment < 0) {
            throw new ApiError(
                StatusCodes.BAD_REQUEST,
                `Không đủ điểm tích lũy để thực hiện giao dịch trừ điểm (Hiện có: ${member.rewardPoints}, yêu cầu trừ: ${Math.abs(adjustment)}).`
            );
        }

        // 3. update rewardPoints
        const updatedMember = await Member.findByIdAndUpdate(
            memberId,
            { $inc: { rewardPoints: adjustment } },
            { new: true, session }
        );

        // 4. save ScoreHistory
        const earnedPoints = adjustment > 0 ? adjustment : 0;
        const usedPoints = adjustment < 0 ? Math.abs(adjustment) : 0;

        await ScoreHistory.create(
            [
                {
                    memberId: member._id,
                    usedPoints,
                    earnedPoints,
                    remainingPoints: updatedMember.rewardPoints,
                    reason: reason || 'Admin điều chỉnh điểm'
                }
            ],
            { session }
        );

        await session.commitTransaction();
        return updatedMember;
    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};

/**
 * update status member account
 * @param {string} memberId 
 * @param {number} status new status (1: Hoạt động, 0: Khóa)
 */
export const updateMemberStatus = async (memberId, status) => {
    const member = await Member.findById(memberId);
    if (!member) return null;

    // update status account
    await Account.findByIdAndUpdate(
        member.accountId,
        { status: Number(status) },
        { new: true }
    );

    // return member after update
    return await Member.findById(memberId).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken'
    }).lean();
};
