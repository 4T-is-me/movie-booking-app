import { Account } from '../models/accountModel.js';
import { Staff } from '../models/staffModel.js';
import bcrypt from 'bcrypt';

export const getStaffList = async (page = 1, limit = 10, status = null) => {
    const skip = (page - 1) * limit;
    const matchAccountStatus = status !== null ? { 'accountInfo.status': parseInt(status) } : {};

    const staffs = await Staff.aggregate([
        {
            $lookup: {
                from: 'accounts',
                localField: 'accountId',
                foreignField: '_id',
                as: 'accountInfo'
            }
        },
        { $unwind: '$accountInfo' },
        { $match: matchAccountStatus },
        {
            $project: {
                'accountInfo.passwordHash': 0,
                'accountInfo.refreshToken': 0
            }
        },
        { $skip: skip },
        { $limit: parseInt(limit) }
    ]);

    const total = await Staff.countDocuments();
    return { staffs, total, totalPages: Math.ceil(total / limit), currentPage: page };
};

export const createStaff = async (accountData, staffData) => {
    // 1. Tạo Account trước
    const hashedPassword = await bcrypt.hash(accountData.password, 10);
    const newAccount = new Account({
        username: accountData.username,
        email: accountData.email,
        passwordHash: hashedPassword,
        roleId: accountData.roleId,
        status: 1
    });
    
    const savedAccount = await newAccount.save();

    // 2. Tạo Staff liên kết với Account
    const newStaff = new Staff({
        ...staffData,
        accountId: savedAccount._id
    });
    
    return await newStaff.save();
};

export const updateStaff = async (staffId, staffData) => {
    return await Staff.findByIdAndUpdate(staffId, staffData, { new: true });
};

/**
 * XÓA MỀM (SOFT DELETE) NHÂN VIÊN (STAFF)
 */
export const softDeleteStaff = async (staffId) => {
    const staffInfo = await Staff.findById(staffId);
    if (!staffInfo) throw new Error('StaffNotFound');

    // Khóa tài khoản đăng nhập (Account status = 0)
    const updatedAccount = await Account.findByIdAndUpdate(
        staffInfo.accountId, 
        { status: 0 }, 
        { new: true }
    );
    
    return updatedAccount;
};
