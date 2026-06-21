import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'
import { Account } from '../models/accountModel.js'
import { member as Member } from '../models/Member.js'
import { jwtProvider } from '../utils/jwtProvider.js'
import bcryptjs from 'bcryptjs'

const register = async (reqBody) => {
    const existedUser = await Account.findOne({ $or: [{ email: reqBody.email }, { username: reqBody.username }] })
    if (existedUser) {
        throw new ApiError(StatusCodes.CONFLICT, 'Email or username already exists')
    }

    const passwordHash = await bcryptjs.hash(reqBody.password, 10)

    const newAccount = new Account({
        username: reqBody.username,
        email: reqBody.email,
        passwordHash: passwordHash,
        fullName: reqBody.fullName,
        gender: reqBody.gender,
        dateOfBirth: reqBody.dateOfBirth,
        phoneNumber: reqBody.phoneNumber,
        identityCard: reqBody.identityCard,
        address: reqBody.address,
        image: reqBody.image,
        roleId: '6a34f14e2f45bb09dc2d5c1a', // Default Role: Customer
        status: 1,
        registerDate: new Date()
    })

    const savedAccount = await newAccount.save()

    const newMember = new Member({
        MEMBER_ID: `MEM${Date.now()}`,
        SCORE: 0,
        ACCOUNT_ID: savedAccount._id
    })
    await newMember.save()

    const safeAccount = savedAccount.toObject()
    delete safeAccount.passwordHash
    if (safeAccount.passwordSalt) delete safeAccount.passwordSalt

    return safeAccount
}

const login = async (reqBody) => {
    const account = await Account.findOne({ email: reqBody.email }).populate('roleId')
    if (!account) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Email or password is incorrect')
    }

    if (account.status === 0) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Account has been locked!')
    }

    const isMatch = await bcryptjs.compare(reqBody.password, account.passwordHash)
    if (!isMatch) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Email or password is incorrect')
    }

    const payload = {
        accountId: account._id.toString(),
        username: account.username,
        role: account.roleId ? account.roleId.name : 'Customer'
    }

    const accessToken = jwtProvider.generateAccessToken(payload)
    const refreshToken = jwtProvider.generateRefreshToken(payload)

    account.refreshToken = refreshToken
    await account.save()

    const safeAccount = account.toObject()
    delete safeAccount.passwordHash
    if (safeAccount.passwordSalt) delete safeAccount.passwordSalt

    return {
        account: safeAccount,
        accessToken,
        refreshToken
    }
}

const getProfile = async (accountId) => {
    const account = await Account.findById(accountId).select('-passwordHash -passwordSalt -refreshToken')
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    return account
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

    const account = await Account.findByIdAndUpdate(accountId, updateData, { new: true }).select('-passwordHash -passwordSalt -refreshToken')

    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    return account
}

export const accountService = {
    register,
    login,
    getProfile,
    updateProfile
}
