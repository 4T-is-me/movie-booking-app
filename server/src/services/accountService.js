import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'
import { Account } from '../models/accountModel.js'
import { Member } from '../models/memberModel.js'
import { jwtProvider } from '../utils/jwtProvider.js'
import bcryptjs from 'bcryptjs'
import '../models/rolesModel.js'

const register = async (reqBody) => {
    const existedUser = await Account.findOne({ $or: [{ email: reqBody.email }, { username: reqBody.username }] })
    if (existedUser) {
        throw new ApiError(StatusCodes.CONFLICT, 'Email or username already exists')
    }

    const existedMember = await Member.findOne({ $or: [{ identityCard: reqBody.identityCard }, { phoneNumber: reqBody.phoneNumber }] })
    if (existedMember) {
        throw new ApiError(StatusCodes.CONFLICT, 'Identity card or phone number already exists')
    }

    const passwordHash = await bcryptjs.hash(reqBody.password, 10)

    const newAccount = new Account({
        username: reqBody.username,
        email: reqBody.email,
        passwordHash: passwordHash,
        roleId: '6a38d663cf6b5ffdb33166f5', // Default Role: Member
        status: 1,
        registerDate: new Date()
    })

    const savedAccount = await newAccount.save()


    try {
        const newMember = new Member({
            accountId: savedAccount._id,
            fullName: reqBody.fullName,
            gender: reqBody.gender,
            dateOfBirth: reqBody.dateOfBirth,
            phoneNumber: reqBody.phoneNumber,
            identityCard: reqBody.identityCard,
            address: reqBody.address,
            image: reqBody.image || null
        })
        const savedMember = await newMember.save()

        const safeAccount = savedAccount.toObject()
        delete safeAccount.passwordHash
        if (safeAccount.passwordSalt) delete safeAccount.passwordSalt

        const safeMember = savedMember.toObject()
        delete safeMember._id

        return { ...safeAccount, ...safeMember }
    } catch (error) {
        await Account.findByIdAndDelete(savedAccount._id)
        if (error.code === 11000) {
            throw new ApiError(StatusCodes.CONFLICT, 'Identity card or phone number already exists')
        }
        throw error
    }
}

const login = async (reqBody) => {
    const loginId = reqBody.email;

    let account = await Account.findOne({ 
        $or: [{ email: loginId }, { username: loginId }] 
    }).populate('roleId');

    if (!account) {
        const member = await Member.findOne({ phoneNumber: loginId });
        if (member) {
            account = await Account.findById(member.accountId).populate('roleId');
        }
    }

    if (!account) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Username, email, phone or password is incorrect')
    }

    if (account.status === 0) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Account has been locked!')
    }

    const isMatch = await bcryptjs.compare(reqBody.password, account.passwordHash)
    if (!isMatch) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Username, email, phone or password is incorrect')
    }

    const payload = {
        accountId: account._id.toString(),
        username: account.username,
        role: account.roleId ? (account.roleId.name || account.roleId.roleName || 'Customer') : 'Customer'
    }

    const accessToken = jwtProvider.generateAccessToken(payload)
    const refreshToken = jwtProvider.generateRefreshToken(payload)

    account.refreshToken = refreshToken
    await account.save()

    const safeAccount = account.toObject()
    delete safeAccount.passwordHash
    if (safeAccount.passwordSalt) delete safeAccount.passwordSalt

    const member = await Member.findOne({ accountId: account._id }).lean()
    if (member) delete member._id

    return {
        account: { ...safeAccount, ...(member || {}) },
        accessToken,
        refreshToken
    }
}

const getProfile = async (accountId) => {
    const account = await Account.findById(accountId).select('-passwordHash -passwordSalt -refreshToken').lean()
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    const member = await Member.findOne({ accountId: accountId }).lean()
    if (member) delete member._id
    return { ...account, ...(member || {}) }
}

const updateProfile = async (accountId, reqBody) => {
    const allowedUpdates = [
        'fullName', 'gender', 'dateOfBirth',
        'phoneNumber', 'identityCard', 'address', 'image'
    ]

    const updateData = {}
    Object.keys(reqBody).forEach(key => {
        if (allowedUpdates.includes(key)) {
            updateData[key] = reqBody[key]
        }
    })

    const member = await Member.findOneAndUpdate({ accountId: accountId }, updateData, { new: true }).lean()
    if (member) delete member._id

    const account = await Account.findById(accountId).select('-passwordHash -passwordSalt -refreshToken').lean()

    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    return { ...account, ...(member || {}) }
}

export const accountService = {
    register,
    login,
    getProfile,
    updateProfile
}
