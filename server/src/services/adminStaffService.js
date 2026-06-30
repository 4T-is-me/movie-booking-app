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
    const { role, roleId } = accountData;
    let targetRoleId = roleId;

    if (role) {
        let lookupName = role;
        if (role.toLowerCase() === 'staff') {
            lookupName = 'Employee';
        }
        const roleDoc = await Role.findOne({
            $or: [
                { name: { $regex: new RegExp(`^${lookupName}$`, 'i') } },
                { roleName: { $regex: new RegExp(`^${lookupName}$`, 'i') } }
            ]
        }).lean();
        if (!roleDoc) {
            throw new Error('Role không tồn tại trong hệ thống.');
        }
        targetRoleId = roleDoc._id;
    } else if (roleId) {
        const roleDoc = await Role.findById(roleId).lean();
        if (!roleDoc) {
            throw new Error('Role không tồn tại trong hệ thống.');
        }
        const rName = roleDoc.roleName || roleDoc.name || '';
        if (rName.toLowerCase() !== 'employee' && rName.toLowerCase() !== 'manager') {
            throw new Error('Role không hợp lệ. Chỉ chấp nhận Staff (Employee) hoặc Manager.');
        }
        targetRoleId = roleDoc._id;
    } else {
        const roleDoc = await Role.findOne({
            $or: [
                { name: 'Employee' },
                { roleName: 'Employee' }
            ]
        }).lean();
        if (!roleDoc) {
            throw new Error('Không tìm thấy Role mặc định cho Staff (Employee).');
        }
        targetRoleId = roleDoc._id;
    }

    // 1. Tạo Account trước
    const hashedPassword = await bcrypt.hash(accountData.password, 10);
    const newAccount = new Account({
        username: accountData.username,
        email: accountData.email,
        passwordHash: hashedPassword,
        roleId: targetRoleId,
        status: 1
    });
    
    const savedAccount = await newAccount.save();

    // 2. Tạo Staff liên kết với Account
    const newStaff = new Staff({
        ...staffData,
        accountId: savedAccount._id
    });
    
    const savedStaff = await newStaff.save();
    
    // Trả về bản ghi đã được populate đầy đủ thông tin
    return await Staff.findById(savedStaff._id).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();
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
