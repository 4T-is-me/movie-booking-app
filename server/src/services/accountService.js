import { StatusCodes } from 'http-status-codes'
import { constants } from '../utils/constants.js'
import mongoose from 'mongoose'
import ApiError from '../utils/ApiError.js'
import { Account } from '../models/accountModel.js'
import { Member } from '../models/memberModel.js'
import { Manager } from '../models/managerModel.js'
import { Staff } from '../models/staffModel.js'
import { jwtProvider } from '../utils/jwtProvider.js'
import bcryptjs from 'bcryptjs'
import { OAuth2Client } from 'google-auth-library'
import { Otp } from '../models/otpModel.js'
import { sendEmail } from '../utils/sendEmail.js'
import { Role } from '../models/rolesModel.js'
import { NotificationService } from './notificationService.js'

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID || 'PLACEHOLDER_CLIENT_ID')

const register = async (reqBody) => {
    const existedEmail = await Account.findOne({ email: reqBody.email })
    if (existedEmail) {
        throw new ApiError(StatusCodes.CONFLICT, 'Email already exists')
    }

    const existedUsername = await Account.findOne({ username: reqBody.username })
    if (existedUsername) {
        throw new ApiError(StatusCodes.CONFLICT, 'Username already exists')
    }

    const existedPhone = await Member.findOne({ phoneNumber: reqBody.phoneNumber })
    if (existedPhone) {
        throw new ApiError(StatusCodes.CONFLICT, 'Phone number already exists')
    }

    const existedIdCard = await Member.findOne({ identityCard: reqBody.identityCard })
    if (existedIdCard) {
        throw new ApiError(StatusCodes.CONFLICT, 'Identity card already exists')
    }

    const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    await Otp.deleteMany({ email: reqBody.email });
    await new Otp({ 
        email: reqBody.email, 
        otp: otpCode,
        type: 'register',
        payload: reqBody
    }).save();

    await sendEmail(
        reqBody.email,
        'Xác thực tài khoản Movie Booking',
        `Mã OTP xác thực tài khoản của bạn là: ${otpCode}. Mã này có hiệu lực trong 5 phút.`
    );

    return { 
        username: reqBody.username,
        email: reqBody.email,
        fullName: reqBody.fullName,
        phoneNumber: reqBody.phoneNumber
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

    const isMatch = await bcryptjs.compare(reqBody.password, account.passwordHash)
    if (!isMatch) {
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Username, email, phone or password is incorrect')
    }

    if (account.status !== 1) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Account has been locked!')
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
    safeAccount.role = payload.role;

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
        .populate('managerInfo')
        .populate('staffInfo')
        .select('-passwordHash -passwordSalt -refreshToken')
        .lean({ virtuals: true });

    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found');
    }

    const fullName = account.memberInfo?.fullName || account.managerInfo?.fullName || account.staffInfo?.fullName;
    const phone = account.memberInfo?.phoneNumber || account.managerInfo?.phoneNumber || account.staffInfo?.phone;
    const identityCard = account.memberInfo?.identityCard || account.managerInfo?.identityCard || account.staffInfo?.identityCard;
    const isStaff = account.roleId?.name === 'Staff' || account.roleId?.roleName === 'Staff';
    const staffInfo = account.staffInfo;

    return {
        accountId: account._id.toString(),
        username: account.username,
        fullName: fullName,
        email: account.email,
        phone: phone,
        address: account.memberInfo?.address || account.staffInfo?.address,
        dob: account.memberInfo?.dateOfBirth,
        gender: account.memberInfo?.gender,
        identityCard: identityCard,
        score: account.memberInfo?.rewardPoints || 0,
        rewardPoints: account.memberInfo?.rewardPoints || 0,
        membershipRank: account.memberInfo?.membershipRank || 'bronze',
        totalSpent: account.memberInfo?.totalSpent || 0,
        role: account.roleId ? (account.roleId.name || account.roleId.roleName || 'Customer') : 'Customer',
        status: account.status,
        registerDate: account.registerDate,
        avatar: account.memberInfo?.image,
        image: account.memberInfo?.image,
        favoriteCinema: account.memberInfo?.favoriteCinema,
        favoriteGenres: account.memberInfo?.favoriteGenres || [],
        kpiPoints: isStaff ? (staffInfo?.kpiPoints || 0) : 0,
        ticketSold: isStaff ? (staffInfo?.ticketSold || 0) : 0,
        rating: isStaff ? (staffInfo?.rating || 0) : 0,
        totalRatings: isStaff ? (staffInfo?.totalRatings || 0) : 0
    };
}

const updateProfile = async (accountId, reqBody) => {
    // Validation
    if (reqBody.fullName !== undefined) {
        if (!reqBody.fullName || reqBody.fullName.trim() === '') {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Full name cannot be empty');
        }
        if (/\d/.test(reqBody.fullName)) {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Full name cannot contain numbers');
        }
    }

    if (reqBody.email) {
        const emailRegex = /^[^\s@]+@gmail\.com$/;
        if (!emailRegex.test(reqBody.email)) {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Email must end with @gmail.com');
        }
        const existedEmail = await Account.findOne({ email: reqBody.email, _id: { $ne: accountId } });
        if (existedEmail) {
            throw new ApiError(StatusCodes.CONFLICT, 'Email already exists');
        }
        await Account.findByIdAndUpdate(accountId, { email: reqBody.email });
    }

    const memberUpdate = {};
    if (reqBody.phone) {
        const phoneRegex = /^0[0-9]{9}$/;
        if (!phoneRegex.test(reqBody.phone)) {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Phone number must be exactly 10 digits and start with 0');
        }
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
        const account = await Account.findById(accountId).populate('roleId');
        const roleName = account.roleId ? (account.roleId.name || account.roleId.roleName || 'Customer') : 'Customer';
        
        if (roleName === constants.ROLES.MANAGER) {
            await Manager.findOneAndUpdate({ accountId: accountId }, memberUpdate);
        } else if (roleName === constants.ROLES.STAFF) {
            const staffUpdate = { ...memberUpdate };
            if (staffUpdate.phoneNumber) {
                staffUpdate.phone = staffUpdate.phoneNumber;
                delete staffUpdate.phoneNumber;
            }
            await Staff.findOneAndUpdate({ accountId: accountId }, staffUpdate);
        } else {
            await Member.findOneAndUpdate({ accountId: accountId }, memberUpdate);
        }
    }

    return await getProfile(accountId);
}

const changePassword = async (accountId, reqBody) => {
    const account = await Account.findById(accountId);
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found');
    }

    const isMatch = await bcryptjs.compare(reqBody.currentPassword, account.passwordHash);
    if (!isMatch) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Mật khẩu hiện tại không đúng');
    }

    const isSame = await bcryptjs.compare(reqBody.newPassword, account.passwordHash);
    if (isSame) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Mật khẩu mới không được trùng với mật khẩu hiện tại');
    }

    const hashedPassword = await bcryptjs.hash(reqBody.newPassword, 10);
    account.passwordHash = hashedPassword;
    await account.save();

    return { success: true };
}

const resetPassword = async (reqBody) => {
    const { email, newPassword } = reqBody;
    const account = await Account.findOne({ email });
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Tài khoản không tồn tại');
    }

    const hashedPassword = await bcryptjs.hash(newPassword, 10);
    account.passwordHash = hashedPassword;
    await account.save();

    return { success: true, message: 'Đổi mật khẩu thành công' };
}

const googleLogin = async (idToken) => {
    try {
        const ticket = await googleClient.verifyIdToken({
            idToken: idToken,
            audience: process.env.GOOGLE_CLIENT_ID || 'PLACEHOLDER_CLIENT_ID'
        });
        const payload = ticket.getPayload();
        const email = payload['email'];
        const name = payload['name'];
        const picture = payload['picture'];

        let account = await Account.findOne({ email }).populate('roleId');

        if (!account) {
            let roleId = '6a38d663cf6b5ffdb33166f5';
            const defaultRole = await Role.findOne({ roleName: 'Member' });
            if (defaultRole) roleId = defaultRole._id;

            const session = await mongoose.startSession();
            try {
                await session.withTransaction(async () => {
                    const passwordHash = await bcryptjs.hash(Math.random().toString(36).slice(-8), 10);
                    const newAccount = new Account({
                        username: email.split('@')[0] + Math.floor(Math.random() * 1000),
                        email: email,
                        passwordHash: passwordHash,
                        roleId: roleId,
                        status: 1,
                        registerDate: new Date()
                    });
                    account = await newAccount.save({ session });

                    const newMember = new Member({
                        accountId: account._id,
                        fullName: name,
                        image: picture
                    });
                    await newMember.save({ session });
                });
            } finally {
                session.endSession();
            }
            
            // Re-fetch to populate role
            account = await Account.findById(account._id).populate('roleId');
        }

        if (account.status !== 1) {
            throw new ApiError(StatusCodes.FORBIDDEN, 'Account has been locked!');
        }

        const jwtPayload = {
            accountId: account._id.toString(),
            username: account.username,
            role: account.roleId ? (account.roleId.name || account.roleId.roleName || 'Customer') : 'Customer'
        };

        const accessToken = jwtProvider.generateAccessToken(jwtPayload);
        const refreshToken = jwtProvider.generateRefreshToken(jwtPayload);

        account.refreshToken = refreshToken;
        await account.save();

        const safeAccount = account.toObject();
        delete safeAccount.passwordHash;
        if (safeAccount.passwordSalt) delete safeAccount.passwordSalt;
        safeAccount.role = jwtPayload.role;

        const member = await Member.findOne({ accountId: account._id }).lean();
        if (member) delete member._id;

        return {
            account: { ...safeAccount, ...(member || {}) },
            accessToken,
            refreshToken
        };
    } catch (error) {
        if (error.statusCode) {
            throw error;
        }
        throw new ApiError(StatusCodes.UNAUTHORIZED, 'Invalid Google token or account creation failed: ' + error.message);
    }
}

const verifyOtp = async (reqBody) => {
    const { email, otp } = reqBody;
    const otpRecord = await Otp.findOne({ email, otp });
    if (!otpRecord) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Mã OTP không hợp lệ hoặc đã hết hạn');
    }

    if (otpRecord.type === 'register') {
        const payload = otpRecord.payload;
        if (!payload) throw new ApiError(StatusCodes.BAD_REQUEST, 'Dữ liệu đăng ký không hợp lệ');

        let roleId = '6a38d663cf6b5ffdb33166f5';
        const defaultRole = await Role.findOne({ roleName: 'Member' });
        if (defaultRole) roleId = defaultRole._id;

        const session = await mongoose.startSession();
        try {
            await session.withTransaction(async () => {
                const passwordHash = await bcryptjs.hash(payload.password, 10);
                const newAccount = new Account({
                    username: payload.username,
                    email: payload.email,
                    passwordHash: passwordHash,
                    roleId: roleId,
                    status: 1,
                    registerDate: new Date()
                });
                const savedAccount = await newAccount.save({ session });

                const newMember = new Member({
                    accountId: savedAccount._id,
                    fullName: payload.fullName,
                    gender: payload.gender,
                    dateOfBirth: payload.dateOfBirth,
                    phoneNumber: payload.phoneNumber,
                    identityCard: payload.identityCard,
                    address: payload.address,
                    image: payload.image || null
                });
                await newMember.save({ session });
            });
        } finally {
            session.endSession();
        }
    } else {
        const account = await Account.findOne({ email });
        if (!account) {
            throw new ApiError(StatusCodes.NOT_FOUND, 'Tài khoản không tồn tại');
        }

        if (account.status === 0) {
            account.status = 1;
            await account.save();
        }
    }

    await Otp.deleteOne({ _id: otpRecord._id });
    return { success: true, message: 'Xác thực thành công' };
}

const sendOtp = async (reqBody) => {
    const { email } = reqBody;
    const account = await Account.findOne({ email });
    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Tài khoản không tồn tại');
    }

    const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    await Otp.deleteMany({ email });
    await new Otp({ email, otp: otpCode, type: 'forgot_password' }).save();

    await sendEmail(
        email,
        'Khôi phục mật khẩu Movie Booking',
        `Mã OTP khôi phục mật khẩu của bạn là: ${otpCode}. Mã này có hiệu lực trong 5 phút.`
    );
    return { success: true, message: 'OTP sent' };
}

/**
 * Saves or updates the FCM device token for the given account.
 * Called after every successful login so the token stays fresh.
 *
 * @param {string} accountId - MongoDB ObjectId of the authenticated account.
 * @param {string} fcmToken  - FCM registration token from the Android/iOS client.
 */
const registerFcmToken = async (accountId, fcmToken) => {
    if (!fcmToken || typeof fcmToken !== 'string' || fcmToken.trim() === '') {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'FCM token is required')
    }

    const trimmedToken = fcmToken.trim()

    // 1. Remove this token from any other accounts' fcmTokens list or legacy fcmToken
    await Account.updateMany(
        { _id: { $ne: accountId }, $or: [{ fcmToken: trimmedToken }, { fcmTokens: trimmedToken }] },
        { 
            $set: { fcmToken: null },
            $pull: { fcmTokens: trimmedToken }
        }
    )

    // 2. Add this token to the current account's fcmTokens and update legacy fcmToken
    const account = await Account.findByIdAndUpdate(
        accountId,
        { 
            fcmToken: trimmedToken,
            $addToSet: { fcmTokens: trimmedToken } 
        },
        { new: true, runValidators: false }
    ).select('_id username fcmToken fcmTokens')

    if (!account) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Account not found')
    }

    // Auto-subscribe the registered token to the 'promotions' topic for broadcasting
    await NotificationService.subscribeToPromotions(trimmedToken)

    return { success: true, message: 'FCM token registered successfully' }
}

export const accountService = {
    register,
    login,
    googleLogin,
    getProfile,
    updateProfile,
    changePassword,
    resetPassword,
    verifyOtp,
    sendOtp,
    registerFcmToken
}
