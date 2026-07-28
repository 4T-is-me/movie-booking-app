import { Inventory } from "../models/inventoryModel.js";
import { InventoryLog } from "../models/inventoryLogModel.js";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";
import mongoose from "mongoose";

const getInventoryByCinema = async (cinemaId) => {
    if (!mongoose.Types.ObjectId.isValid(cinemaId)) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Invalid Cinema ID");
    }
    return await Inventory.find({ cinemaId: new mongoose.Types.ObjectId(cinemaId) })
        .sort({ itemCode: 1 });
};

const refillInventory = async (data) => {
    const { cinemaId, itemCode, quantity, reason } = data;

    if (!cinemaId || !itemCode || quantity === undefined) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Missing required fields");
    }

    const session = await mongoose.startSession();
    session.startTransaction();

    try {
        const inv = await Inventory.findOne({
            cinemaId: new mongoose.Types.ObjectId(cinemaId),
            itemCode
        }).session(session);

        if (!inv) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Inventory item not found for this cinema");
        }

        const oldQuantity = inv.quantity;
        const newQuantity = oldQuantity + quantity;

        // Update quantity
        inv.quantity = newQuantity;
        await inv.save({ session });

        // Create log
        await InventoryLog.create([{
            cinemaId,
            itemCode,
            type: 'REFILL',
            changeQuantity: quantity,
            oldQuantity,
            newQuantity,
            reason: reason || "Admin refill"
        }], { session });

        await session.commitTransaction();
        return inv;
    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};

export const adminInventoryService = {
    getInventoryByCinema,
    refillInventory
};
