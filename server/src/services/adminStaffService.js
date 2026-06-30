import { Account } from '../models/accountModel.js';
import { Staff } from '../models/staffModel.js';
import { Role } from '../models/rolesModel.js';
import mongoose from 'mongoose';
import bcrypt from 'bcryptjs';

export const getStaffList = async (arg1, arg2, arg3) => {
    let page = 1;
    let limit = 10;
    let status = null;
    let role = null;
    let search = null;

    if (typeof arg1 === 'object' && arg1 !== null) {
        page = arg1.page !== undefined ? arg1.page : 1;
        limit = arg1.limit !== undefined ? arg1.limit : 10;
        status = arg1.status !== undefined ? arg1.status : null;
        role = arg1.role !== undefined ? arg1.role : null;
        search = arg1.search !== undefined ? arg1.search : null;
    } else {
        page = arg1 !== undefined ? arg1 : 1;
        limit = arg2 !== undefined ? arg2 : 10;
        status = arg3 !== undefined ? arg3 : null;
    }

    const skip = (page - 1) * limit;
    const accountFilter = {};
    let shouldFilterByAccount = false;

    if (status !== null && status !== undefined && status !== '') {
        accountFilter.status = Number(status);
        shouldFilterByAccount = true;
    }

    if (role !== null && role !== undefined && role !== '') {
        shouldFilterByAccount = true;
        if (mongoose.Types.ObjectId.isValid(role)) {
            accountFilter.roleId = role;
        } else {
            const roleDoc = await Role.findOne({
                $or: [
                    { name: { $regex: new RegExp(`^${role}$`, 'i') } },
                    { roleName: { $regex: new RegExp(`^${role}$`, 'i') } }
                ]
            });
            if (roleDoc) {
                accountFilter.roleId = roleDoc._id;
            } else {
                accountFilter.roleId = new mongoose.Types.ObjectId();
            }
        }
    }

    const staffFilter = {};

    if (shouldFilterByAccount) {
        if (search && search.trim() !== '') {
            const cleanSearch = search.trim();
            const possibleAccounts = await Account.find(accountFilter).select('_id');
            const possibleAccountIds = possibleAccounts.map(a => a._id);

            const searchAccounts = await Account.find({
                ...accountFilter,
                email: { $regex: cleanSearch, $options: 'i' }
            }).select('_id');
            const searchAccountIds = searchAccounts.map(a => a._id);

            staffFilter.accountId = { $in: possibleAccountIds };
            staffFilter.$or = [
                { fullName: { $regex: cleanSearch, $options: 'i' } },
                { accountId: { $in: searchAccountIds } }
            ];
        } else {
            const matchingAccounts = await Account.find(accountFilter).select('_id');
            const matchingAccountIds = matchingAccounts.map(a => a._id);
            staffFilter.accountId = { $in: matchingAccountIds };
        }
    } else {
        if (search && search.trim() !== '') {
            const cleanSearch = search.trim();
            const searchAccounts = await Account.find({
                email: { $regex: cleanSearch, $options: 'i' }
            }).select('_id');
            const searchAccountIds = searchAccounts.map(a => a._id);

            staffFilter.$or = [
                { fullName: { $regex: cleanSearch, $options: 'i' } },
                { accountId: { $in: searchAccountIds } }
            ];
        }
    }

    const total = await Staff.countDocuments(staffFilter);
    const staffs = await Staff.find(staffFilter)
        .populate({
            path: 'accountId',
            select: '-passwordHash -refreshToken',
            populate: {
                path: 'roleId',
                select: 'roleName name'
            }
        })
        .skip(skip)
        .limit(Number(limit))
        .lean();

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
