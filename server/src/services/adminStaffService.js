import { Account } from '../models/accountModel.js';
import { Staff } from '../models/staffModel.js';
import { Manager } from '../models/managerModel.js';
import { Role } from '../models/rolesModel.js';
import mongoose from 'mongoose';
import bcrypt from 'bcryptjs';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';

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
            let orQuery = [
                { name: { $regex: new RegExp(`^${role}$`, 'i') } },
                { roleName: { $regex: new RegExp(`^${role}$`, 'i') } }
            ];
            if (role.toLowerCase() === 'staff' || role.toLowerCase() === 'employee') {
                orQuery.push({ name: { $regex: new RegExp(`^Employee$`, 'i') } });
                orQuery.push({ roleName: { $regex: new RegExp(`^Employee$`, 'i') } });
                orQuery.push({ name: { $regex: new RegExp(`^Staff$`, 'i') } });
                orQuery.push({ roleName: { $regex: new RegExp(`^Staff$`, 'i') } });
            }
            
            const roleDocs = await Role.find({ $or: orQuery });
            if (roleDocs && roleDocs.length > 0) {
                accountFilter.roleId = { $in: roleDocs.map(r => r._id) };
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

    const staffs = await Staff.find(staffFilter)
        .populate({
            path: 'accountId',
            select: '-passwordHash -refreshToken',
            populate: {
                path: 'roleId',
                select: 'roleName name'
            }
        })
        .lean();

    let mappedManagers = [];
    if (!role || (typeof role === 'string' && role.toLowerCase() === 'manager')) {
        const managersList = await Manager.find(staffFilter)
            .populate({
                path: 'accountId',
                select: '-passwordHash -refreshToken',
                populate: {
                    path: 'roleId',
                    select: 'roleName name'
                }
            })
            .lean();
            
        mappedManagers = managersList.map(m => {
            const copy = { ...m };
            copy.phone = m.phoneNumber;
            copy.position = 'Manager';
            return copy;
        });
    }

    const allRecords = [...staffs, ...mappedManagers];
    const total = allRecords.length;
    const paginated = allRecords.slice(skip, skip + Number(limit));

    return { staffs: paginated, total, totalPages: Math.ceil(total / limit), currentPage: page };
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
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Role does not exist in the system.');
        }
        targetRoleId = roleDoc._id;
    } else if (roleId) {
        const roleDoc = await Role.findById(roleId).lean();
        if (!roleDoc) {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Role does not exist in the system.');
        }
        const rName = roleDoc.roleName || roleDoc.name || '';
        if (rName.toLowerCase() !== 'employee' && rName.toLowerCase() !== 'manager') {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Invalid role. Only Staff (Employee) or Manager are accepted.');
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
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Default role for Staff (Employee) not found.');
        }
        targetRoleId = roleDoc._id;
    }

    const isManager = !!(role && role.toLowerCase() === 'manager');

    // --- Pre-checks BEFORE database mutation ---
    // 1. Check duplicate username or email in Account
    const existingAccount = await Account.findOne({
        $or: [
            { username: { $regex: new RegExp(`^${accountData.username.trim()}$`, 'i') } },
            { email: { $regex: new RegExp(`^${accountData.email.trim()}$`, 'i') } }
        ]
    });
    if (existingAccount) {
        if (existingAccount.username.toLowerCase() === accountData.username.trim().toLowerCase()) {
            throw new ApiError(StatusCodes.CONFLICT, 'Username already exists.');
        } else {
            throw new ApiError(StatusCodes.CONFLICT, 'Email already exists.');
        }
    }

    if (isManager) {
        // 2. Check if cinema already has a Manager assigned
        const existingManagerForCinema = await Manager.findOne({
            cinemaId: staffData.cinemaId,
            isDeleted: { $ne: true }
        });
        if (existingManagerForCinema) {
            throw new ApiError(StatusCodes.CONFLICT, 'This cinema already has an assigned Manager.');
        }

        // 3. Check duplicate phone or identityCard in Managers
        const existingMgrPhoneOrCard = await Manager.findOne({
            $or: [
                { phoneNumber: staffData.phone },
                { identityCard: staffData.identityCard }
            ]
        });
        if (existingMgrPhoneOrCard) {
            if (existingMgrPhoneOrCard.phoneNumber === staffData.phone) {
                throw new ApiError(StatusCodes.CONFLICT, 'Phone number already exists.');
            } else {
                throw new ApiError(StatusCodes.CONFLICT, 'Identity card already exists.');
            }
        }
    } else {
        // Check duplicate identityCard in Staff
        const existingStaff = await Staff.findOne({ identityCard: staffData.identityCard });
        if (existingStaff) {
            throw new ApiError(StatusCodes.CONFLICT, 'Identity card already exists.');
        }
    }

    // 1. Create Account first
    const hashedPassword = await bcrypt.hash(accountData.password, 10);
    const newAccount = new Account({
        username: accountData.username.trim(),
        email: accountData.email.trim(),
        passwordHash: hashedPassword,
        roleId: targetRoleId,
        status: 1
    });

    const savedAccount = await newAccount.save();

    // 2. Create Staff or Manager linked with Account
    let savedRecord;
    try {
        if (isManager) {
            const newManager = new Manager({
                accountId: savedAccount._id,
                cinemaId: staffData.cinemaId,
                fullName: staffData.fullName,
                phoneNumber: staffData.phone,
                identityCard: staffData.identityCard,
                salary: staffData.salary,
                revenueTarget: 100000000, // Default revenue target for new managers
                startDate: staffData.startDate
            });
            savedRecord = await newManager.save();
        } else {
            const newStaff = new Staff({
                ...staffData,
                accountId: savedAccount._id
            });
            savedRecord = await newStaff.save();
        }
    } catch (saveError) {
        // Rollback saved account on record failure
        await Account.findByIdAndDelete(savedAccount._id);
        if (saveError.code === 11000) {
            throw new ApiError(StatusCodes.CONFLICT, 'Data already exists (Username, Email, Identity Card, Phone, or Cinema already has a Manager).');
        }
        throw saveError;
    }

    // Return record populated with full information
    const ModelClass = isManager ? Manager : Staff;
    const finalRecord = await ModelClass.findById(savedRecord._id).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();

    if (isManager && finalRecord) {
        finalRecord.phone = finalRecord.phoneNumber;
        finalRecord.position = 'Manager';
    }
    return finalRecord;
};

export const updateStaff = async (staffId, updateData) => {
    let staff = await Staff.findById(staffId);
    let isManagerModel = false;
    if (!staff) {
        staff = await Manager.findById(staffId);
        if (!staff) return null;
        isManagerModel = true;
    }

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

    // 2. Update Staff/Manager fields
    let updatedStaff;
    if (isManagerModel) {
        if (staffFields.phone) {
            staffFields.phoneNumber = staffFields.phone;
            delete staffFields.phone;
        }
        updatedStaff = await Manager.findByIdAndUpdate(
            staffId,
            { $set: staffFields },
            { new: true }
        );
    } else {
        updatedStaff = await Staff.findByIdAndUpdate(
            staffId,
            { $set: staffFields },
            { new: true }
        );
    }

    // Return record populated with full information after updating
    const ModelClass = isManagerModel ? Manager : Staff;
    const finalRecord = await ModelClass.findById(updatedStaff._id).populate({
        path: 'accountId',
        select: '-passwordHash -refreshToken',
        populate: {
            path: 'roleId',
            select: 'roleName name'
        }
    }).lean();

    if (isManagerModel && finalRecord) {
        finalRecord.phone = finalRecord.phoneNumber;
        finalRecord.position = 'Manager';
    }
    return finalRecord;
};

/**
 * SOFT DELETE STAFF
 */
export const softDeleteStaff = async (staffId) => {
    let staffInfo = await Staff.findById(staffId);
    if (!staffInfo) {
        staffInfo = await Manager.findById(staffId);
        if (!staffInfo) throw new Error('StaffNotFound');
    }

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
