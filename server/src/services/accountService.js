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

    const salt = await bcryptjs.genSalt(10)
    const passwordHash = await bcryptjs.hash(reqBody.password, salt)

    const newAccount = new Account({
        username: reqBody.username,
        email: reqBody.email,
        passwordHash: passwordHash,
        passwordSalt: salt,
        refreshToken: null,
        fullName: reqBody.fullName,
        gender: reqBody.gender,
        dateOfBirth: reqBody.dateOfBirth,
        phoneNumber: reqBody.phoneNumber,
        identityCard: reqBody.identityCard,
        address: reqBody.address,
        image: reqBody.image,
        roleId: reqBody.roleId,
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
    delete safeAccount.passwordSalt

    return safeAccount
}

const login = async (reqBody) => {
    const account = await Account.findOne({ email: reqBody.email })
    if (!account) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Email or password is incorrect')
    }

    const isMatch = bcryptjs.compareSync(reqBody.password, account.passwordHash)
    if (!isMatch) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Email or password is incorrect')
    }

    if (account.status === 0) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Account has been locked!')
    }

    const payload = {
        sub: account._id.toString(),
        email: account.email,
        roleId: account.roleId
    }

    const accessToken = jwtProvider.generateAccessToken(payload)
    const refreshToken = jwtProvider.generateRefreshToken(payload)

    account.refreshToken = refreshToken
    await account.save()

    const safeAccount = account.toObject()
    delete safeAccount.passwordHash
    delete safeAccount.passwordSalt

    return {
        account: safeAccount,
        accessToken,
        refreshToken
    }
}

const getProfile = async (accountId) => {
    const account = await Account.findById(accountId)
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    const safeAccount = account.toObject()
    delete safeAccount.passwordHash
    delete safeAccount.passwordSalt
    return safeAccount
}

const updateProfile = async (accountId, reqBody) => {
    const account = await Account.findByIdAndUpdate(accountId, reqBody, { new: true })
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }
    const safeAccount = account.toObject()
    delete safeAccount.passwordHash
    delete safeAccount.passwordSalt
    return safeAccount
}

export const accountService = {
    register,
    login,
    getProfile,
    updateProfile
}
