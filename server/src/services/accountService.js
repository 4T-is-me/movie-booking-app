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
    const account = await Account.findById(accountId)
        .populate('roleId')
        .populate('memberInfo')
        .select('-passwordHash -passwordSalt -refreshToken')
        .lean({ virtuals: true });

    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found');
    }

    return {
        username: account.username,
        fullName: account.memberInfo?.fullName,
        email: account.email,
        phone: account.memberInfo?.phoneNumber,
        address: account.memberInfo?.address,
        dob: account.memberInfo?.dateOfBirth,
        gender: account.memberInfo?.gender,
        identityCard: account.memberInfo?.identityCard,
        score: account.memberInfo?.rewardPoints || 0,
        role: account.roleId ? (account.roleId.name || account.roleId.roleName || 'Customer') : 'Customer',
        status: account.status,
        registerDate: account.registerDate,
        avatar: account.memberInfo?.image,
        favoriteCinema: account.memberInfo?.favoriteCinema,
        favoriteGenres: account.memberInfo?.favoriteGenres || []
    };
}

const updateProfile = async (accountId, reqBody) => {
    if (reqBody.email) {
        const existedEmail = await Account.findOne({ email: reqBody.email, _id: { $ne: accountId } });
        if (existedEmail) {
            throw new ApiError(StatusCodes.CONFLICT, 'Email already exists');
        }
        await Account.findByIdAndUpdate(accountId, { email: reqBody.email });
    }

    const memberUpdate = {};
    if (reqBody.phone) {
        const existedPhone = await Member.findOne({ phoneNumber: reqBody.phone, accountId: { $ne: accountId } });
        if (existedPhone) {
            throw new ApiError(StatusCodes.CONFLICT, 'Phone number already exists');
        }
        memberUpdate.phoneNumber = reqBody.phone;
    }
    if (reqBody.identityCard) {
        const existedCard = await Member.findOne({ identityCard: reqBody.identityCard, accountId: { $ne: accountId } });
        if (existedCard) {
            throw new ApiError(StatusCodes.CONFLICT, 'Identity card already exists');
        }
        memberUpdate.identityCard = reqBody.identityCard;
    }
    if (reqBody.fullName !== undefined) {
        memberUpdate.fullName = reqBody.fullName;
    }
    if (reqBody.address !== undefined) {
        memberUpdate.address = reqBody.address;
    }
    if (reqBody.dob !== undefined) {
        memberUpdate.dateOfBirth = reqBody.dob;
    }
    if (reqBody.gender !== undefined) {
        memberUpdate.gender = reqBody.gender;
    }
    if (reqBody.favoriteCinema !== undefined) {
        memberUpdate.favoriteCinema = reqBody.favoriteCinema;
    }
    if (reqBody.favoriteGenres !== undefined) {
        memberUpdate.favoriteGenres = reqBody.favoriteGenres;
    }
    if (reqBody.avatar !== undefined) {
        memberUpdate.image = reqBody.avatar;
    }

    if (Object.keys(memberUpdate).length > 0) {
        await Member.findOneAndUpdate({ accountId: accountId }, memberUpdate);
    }

    return await getProfile(accountId);
}

export const accountService = {
    register,
    login,
    getProfile,
    updateProfile
}
