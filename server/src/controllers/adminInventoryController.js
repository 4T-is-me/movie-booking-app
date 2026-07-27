import { adminInventoryService } from "../services/adminInventoryService.js";
import { StatusCodes } from "http-status-codes";

const getInventory = async (req, res, next) => {
    try {
        const { cinemaId } = req.params;
        const inventory = await adminInventoryService.getInventoryByCinema(cinemaId);
        res.status(StatusCodes.OK).json({ success: true, data: inventory });
    } catch (error) {
        next(error);
    }
};

const refill = async (req, res, next) => {
    try {
        const result = await adminInventoryService.refillInventory(req.body);
        res.status(StatusCodes.OK).json({ success: true, data: result, message: "Inventory refilled successfully" });
    } catch (error) {
        next(error);
    }
};

export const adminInventoryController = {
    getInventory,
    refill
};
