import { Supplier } from "../models/supplierModel.js";
import { ImportOrder } from "../models/importOrderModel.js";
import { Inventory } from "../models/inventoryModel.js";
import { InventoryLog } from "../models/inventoryLogModel.js";
import { Manager } from "../models/managerModel.js";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";
import mongoose from "mongoose";

// ================================
// SUPPLIER
// ================================

const getAllSuppliers = async () => {
    return await Supplier.find({ isActive: true }).sort({ name: 1 });
};

const createSupplier = async (data) => {
    const { name, phone, email, address } = data;
    if (!name || !phone) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Supplier name and phone are required");
    }
    return await Supplier.create({ name, phone, email, address });
};

// ================================
// IMPORT ORDER - MANAGER
// ================================

const getImportOrdersByManager = async (accountId, query = {}) => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) throw new ApiError(StatusCodes.FORBIDDEN, "Manager profile not found");

    const filter = { cinemaId: manager.cinemaId };
    if (query.status) filter.status = query.status;

    return await ImportOrder.find(filter)
        .populate('supplierId', 'name phone')
        .populate('cinemaId', 'cinemaName')
        .populate('managerId', 'fullName phoneNumber')
        .sort({ createdAt: -1 });
};

const createImportOrder = async (accountId, data) => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) throw new ApiError(StatusCodes.FORBIDDEN, "Manager profile not found");

    const { supplierId, items, notes } = data;

    if (!supplierId || !items || items.length === 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "supplierId and at least one item are required");
    }

    // Validate supplier exists
    const supplier = await Supplier.findById(supplierId);
    if (!supplier || !supplier.isActive) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Supplier not found or inactive");
    }

    // Validate all itemCodes exist in inventory for this cinema
    for (const item of items) {
        if (!item.itemCode || !item.itemName || item.quantity <= 0 || item.purchasePrice < 0) {
            throw new ApiError(StatusCodes.BAD_REQUEST, `Invalid item data for: ${item.itemCode}`);
        }
        const inv = await Inventory.findOne({ cinemaId: manager.cinemaId, itemCode: item.itemCode });
        if (!inv) {
            throw new ApiError(StatusCodes.NOT_FOUND, `Item ${item.itemCode} not found in inventory for this cinema`);
        }
    }

    // Calculate total
    const totalAmount = items.reduce((sum, item) => sum + item.quantity * item.purchasePrice, 0);

    // Generate import code: PN-YYYYMMDD-XXXX
    const now = new Date();
    const datePart = now.toISOString().slice(0, 10).replace(/-/g, '');
    const countToday = await ImportOrder.countDocuments({
        createdAt: { $gte: new Date(now.setHours(0, 0, 0, 0)) }
    });
    const importCode = `PN-${datePart}-${String(countToday + 1).padStart(4, '0')}`;

    const newOrder = await ImportOrder.create({
        importCode,
        cinemaId: manager.cinemaId,
        managerId: manager._id,
        supplierId,
        items,
        totalAmount,
        notes: notes || '',
        status: 'PENDING'
    });

    return await ImportOrder.findById(newOrder._id)
        .populate('supplierId', 'name phone')
        .populate('cinemaId', 'cinemaName')
        .populate('managerId', 'fullName phoneNumber');
};

const cancelImportOrder = async (accountId, orderId) => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) throw new ApiError(StatusCodes.FORBIDDEN, "Manager profile not found");

    const order = await ImportOrder.findOne({ _id: orderId, cinemaId: manager.cinemaId });
    if (!order) throw new ApiError(StatusCodes.NOT_FOUND, "Import order not found");
    if (order.status !== 'PENDING') {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Cannot cancel an order with status: ${order.status}`);
    }

    order.status = 'CANCELLED';
    await order.save();
    return await ImportOrder.findById(order._id)
        .populate('supplierId', 'name phone')
        .populate('cinemaId', 'cinemaName')
        .populate('managerId', 'fullName phoneNumber');
};

// ================================
// IMPORT ORDER - ADMIN
// ================================

const getAllImportOrders = async (query = {}) => {
    const filter = {};
    if (query.status) filter.status = query.status;
    if (query.cinemaId) filter.cinemaId = query.cinemaId;

    return await ImportOrder.find(filter)
        .populate('supplierId', 'name phone')
        .populate('cinemaId', 'cinemaName')
        .populate('managerId', 'fullName phoneNumber')
        .sort({ createdAt: -1 });
};

const approveImportOrder = async (adminAccountId, orderId) => {
    const order = await ImportOrder.findById(orderId);
    if (!order) throw new ApiError(StatusCodes.NOT_FOUND, "Import order not found");
    if (order.status !== 'PENDING') {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Cannot approve an order with status: ${order.status}`);
    }

    const session = await mongoose.startSession();
    session.startTransaction();

    try {
        // 1. Update order status
        order.status = 'APPROVED';
        order.approvedBy = adminAccountId;
        await order.save({ session });

        // 2. Update inventory & log for each item
        for (const item of order.items) {
            const inv = await Inventory.findOne({
                cinemaId: order.cinemaId,
                itemCode: item.itemCode
            }).session(session);

            if (!inv) {
                throw new ApiError(StatusCodes.NOT_FOUND, `Inventory item not found: ${item.itemCode}`);
            }

            const oldQuantity = inv.quantity;
            const newQuantity = oldQuantity + item.quantity;
            inv.quantity = newQuantity;
            await inv.save({ session });

            await InventoryLog.create([{
                cinemaId: order.cinemaId,
                itemCode: item.itemCode,
                type: 'REFILL',
                changeQuantity: item.quantity,
                oldQuantity,
                newQuantity,
                referenceId: order._id,
                reason: `Goods Intake from order ${order.importCode}`
            }], { session });
        }

        await session.commitTransaction();
        return await ImportOrder.findById(order._id)
            .populate('supplierId', 'name phone')
            .populate('cinemaId', 'cinemaName')
            .populate('managerId', 'fullName phoneNumber');
    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};

const rejectImportOrder = async (adminAccountId, orderId, rejectionReason) => {
    const order = await ImportOrder.findById(orderId);
    if (!order) throw new ApiError(StatusCodes.NOT_FOUND, "Import order not found");
    if (order.status !== 'PENDING') {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Cannot reject an order with status: ${order.status}`);
    }

    order.status = 'REJECTED';
    order.approvedBy = adminAccountId;
    order.rejectionReason = rejectionReason || '';
    await order.save();
    return await ImportOrder.findById(order._id)
        .populate('supplierId', 'name phone')
        .populate('cinemaId', 'cinemaName')
        .populate('managerId', 'fullName phoneNumber');
};

const getManagerInventory = async (accountId) => {
    const manager = await Manager.findOne({ accountId });
    if (!manager) throw new ApiError(StatusCodes.FORBIDDEN, "Manager profile not found");
    return await Inventory.find({ cinemaId: manager.cinemaId }).sort({ itemName: 1 });
};

export const importOrderService = {
    getAllSuppliers,
    createSupplier,
    getImportOrdersByManager,
    createImportOrder,
    cancelImportOrder,
    getAllImportOrders,
    approveImportOrder,
    rejectImportOrder,
    getManagerInventory
};
