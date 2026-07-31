import { importOrderService } from "../services/importOrderService.js";
import { StatusCodes } from "http-status-codes";

// ================================
// SUPPLIER
// ================================

const getSuppliers = async (req, res, next) => {
    try {
        const suppliers = await importOrderService.getAllSuppliers();
        res.status(StatusCodes.OK).json({ success: true, data: suppliers });
    } catch (error) {
        next(error);
    }
};

const createSupplier = async (req, res, next) => {
    try {
        const supplier = await importOrderService.createSupplier(req.body);
        res.status(StatusCodes.CREATED).json({ success: true, data: supplier, message: "Supplier created successfully" });
    } catch (error) {
        next(error);
    }
};

// ================================
// IMPORT ORDER - MANAGER
// ================================

const getManagerImportOrders = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const orders = await importOrderService.getImportOrdersByManager(accountId, req.query);
        res.status(StatusCodes.OK).json({ success: true, data: orders });
    } catch (error) {
        next(error);
    }
};

const createImportOrder = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const order = await importOrderService.createImportOrder(accountId, req.body);
        res.status(StatusCodes.CREATED).json({ success: true, data: order, message: "Import order created successfully" });
    } catch (error) {
        next(error);
    }
};

const cancelImportOrder = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const { id } = req.params;
        const order = await importOrderService.cancelImportOrder(accountId, id);
        res.status(StatusCodes.OK).json({ success: true, data: order, message: "Import order cancelled" });
    } catch (error) {
        next(error);
    }
};

// ================================
// IMPORT ORDER - ADMIN
// ================================

const getAllImportOrders = async (req, res, next) => {
    try {
        const orders = await importOrderService.getAllImportOrders(req.query);
        res.status(StatusCodes.OK).json({ success: true, data: orders });
    } catch (error) {
        next(error);
    }
};

const approveImportOrder = async (req, res, next) => {
    try {
        const adminAccountId = req.user.accountId;
        const { id } = req.params;
        const order = await importOrderService.approveImportOrder(adminAccountId, id);
        res.status(StatusCodes.OK).json({ success: true, data: order, message: "Import order approved and inventory updated" });
    } catch (error) {
        next(error);
    }
};

const rejectImportOrder = async (req, res, next) => {
    try {
        const adminAccountId = req.user.accountId;
        const { id } = req.params;
        const { rejectionReason } = req.body;
        const order = await importOrderService.rejectImportOrder(adminAccountId, id, rejectionReason);
        res.status(StatusCodes.OK).json({ success: true, data: order, message: "Import order rejected" });
    } catch (error) {
        next(error);
    }
};

const getManagerInventory = async (req, res, next) => {
    try {
        const accountId = req.user.accountId;
        const inventory = await importOrderService.getManagerInventory(accountId);
        res.status(StatusCodes.OK).json({ success: true, data: inventory });
    } catch (error) {
        next(error);
    }
};

export const importOrderController = {
    getSuppliers,
    createSupplier,
    getManagerImportOrders,
    createImportOrder,
    cancelImportOrder,
    getAllImportOrders,
    approveImportOrder,
    rejectImportOrder,
    getManagerInventory
};
