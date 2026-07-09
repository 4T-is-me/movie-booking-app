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
            throw new Error('Role does not exist in the system.');
        }
        targetRoleId = roleDoc._id;
    } else if (roleId) {
        const roleDoc = await Role.findById(roleId).lean();
        if (!roleDoc) {
            throw new Error('Role does not exist in the system.');
        }
        const rName = roleDoc.roleName || roleDoc.name || '';
        if (rName.toLowerCase() !== 'employee' && rName.toLowerCase() !== 'manager') {
            throw new Error('Invalid role. Only Staff (Employee) or Manager are accepted.');
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
            throw new Error('Default role for Staff (Employee) not found.');
        }
        targetRoleId = roleDoc._id;
    }

    // 1. Create Account first
    const hashedPassword = await bcrypt.hash(accountData.password, 10);
    const newAccount = new Account({
        username: accountData.username,
        email: accountData.email,
        passwordHash: hashedPassword,
        roleId: targetRoleId,
        status: 1
    });
    
    const savedAccount = await newAccount.save();

    // 2. Create Staff linked with Account
    const newStaff = new Staff({
        ...staffData,
        accountId: savedAccount._id
    });
    
    const savedStaff = await newStaff.save();
    
    // Return record populated with full information
    return await Staff.findById(savedStaff._id).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();
};

export const updateStaff = async (staffId, updateData) => {
    const staff = await Staff.findById(staffId);
    if (!staff) return null;

    const { email, status, ...staffFields } = updateData;
 
    // 1. Update Account email and status if provided
    const accountUpdates = {};
    if (email) {
        const existingAccount = await Account.findOne({ email, _id: { $ne: staff.accountId } });
        if (existingAccount) {
            throw new Error('EmailAlreadyExists');
        }
        accountUpdates.email = email;
    }
    if (status !== undefined) {
        accountUpdates.status = Number(status);
    }
    if (Object.keys(accountUpdates).length > 0) {
        await Account.findByIdAndUpdate(staff.accountId, accountUpdates);
    }

    // 2. Update Staff fields
    const updatedStaff = await Staff.findByIdAndUpdate(
        staffId,
        { $set: staffFields },
        { new: true }
    );

    // Return record populated with full information after updating
    return await Staff.findById(updatedStaff._id).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();
};

/**
 * SOFT DELETE STAFF
 */
export const softDeleteStaff = async (staffId) => {
    const staffInfo = await Staff.findById(staffId);
    if (!staffInfo) throw new Error('StaffNotFound');

    // Lock login account (Account status = 0)
    const updatedAccount = await Account.findByIdAndUpdate(
        staffInfo.accountId, 
        { status: 0 }, 
        { new: true }
    );
    
    return updatedAccount;
};

export const updateStaffStatus = async (staffId, status) => {
    const staff = await Staff.findById(staffId);
    if (!staff) return null;

    // Update status in Account collection
    await Account.findByIdAndUpdate(
        staff.accountId,
        { status: Number(status) },
        { new: true }
    );

    // Return staff record with full information after updating
    return await Staff.findById(staffId).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();
};

export const updateStaffRole = async (staffId, { role, roleId }) => {
    const staff = await Staff.findById(staffId);
    if (!staff) return null;

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
            throw new Error('RoleNotFound');
        }
        targetRoleId = roleDoc._id;
    } else if (roleId) {
        const roleDoc = await Role.findById(roleId).lean();
        if (!roleDoc) {
            throw new Error('RoleNotFound');
        }
        const rName = roleDoc.roleName || roleDoc.name || '';
        if (
            rName.toLowerCase() !== 'employee' &&
            rName.toLowerCase() !== 'manager' &&
            rName.toLowerCase() !== 'admin'
        ) {
            throw new Error('InvalidRole');
        }
        targetRoleId = roleDoc._id;
    }

    // Update roleId in Account collection
    await Account.findByIdAndUpdate(
        staff.accountId,
        { roleId: targetRoleId },
        { new: true }
    );

    // Return staff record with full information after updating
    return await Staff.findById(staffId).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();
};
