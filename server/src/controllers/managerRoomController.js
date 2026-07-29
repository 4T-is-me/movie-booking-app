import { managerRoomService } from "../services/managerRoomService.js";
import { StatusCodes } from "http-status-codes";
import { Manager } from "../models/managerModel.js";

//GET /api/manager/rooms get rooms by manager's cinema
const getManagerRooms = async (req, res, next) => {
    try {
        const manager = await Manager.findOne({ accountId: req.user.accountId });
        if (!manager) {
            return res.status(StatusCodes.FORBIDDEN).json({ success: false, message: "Manager profile not found!" });
        }
        
        const rooms = await managerRoomService.handleGetManagerRooms(manager.cinemaId);
        return res.status(StatusCodes.OK).json({success: true, data: rooms});
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

//GET /api/manager/rooms/:id/seats - get seats of a room
const getRoomSeats = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const manager = await Manager.findOne({ accountId: req.user.accountId });
        if (!manager) {
            return res.status(StatusCodes.FORBIDDEN).json({ success: false, message: "Manager profile not found!" });
        }

        const seatsMatrix = await managerRoomService.handleGetRoomSeats(roomId, manager.cinemaId);
        return res.status(StatusCodes.OK).json({success: true, data: seatsMatrix})
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

const updateRoom = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const data = req.body;
        const manager = await Manager.findOne({ accountId: req.user.accountId });
        if (!manager) {
            return res.status(StatusCodes.FORBIDDEN).json({ success: false, message: "Manager profile not found!" });
        }
        await managerRoomService.handleUpdateRoom(roomId, manager.cinemaId, data);
        return res.status(StatusCodes.OK).json({ success: true, message: "Update room successfully" });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

const updateSeatStatus = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const seatId = req.params.seatId;
        const { status } = req.body;
        const manager = await Manager.findOne({ accountId: req.user.accountId });
        if (!manager) {
            return res.status(StatusCodes.FORBIDDEN).json({ success: false, message: "Manager profile not found!" });
        }
        const updatedSeat = await managerRoomService.handleUpdateSeatStatus(roomId, seatId, manager.cinemaId, status);
        return res.status(StatusCodes.OK).json({ success: true, message: "Update seat status successfully", data: updatedSeat });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

const bulkUpdateSeatStatus = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const { seatIds, status } = req.body;
        const manager = await Manager.findOne({ accountId: req.user.accountId });
        if (!manager) {
            return res.status(StatusCodes.FORBIDDEN).json({ success: false, message: "Manager profile not found!" });
        }
        const result = await managerRoomService.handleBulkUpdateSeatStatus(roomId, seatIds, manager.cinemaId, status);
        return res.status(StatusCodes.OK).json({ success: true, message: "Bulk update seat status successfully", data: result });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

export const managerRoomController = {
    getManagerRooms,
    getRoomSeats,
    updateRoom,
    updateSeatStatus,
    bulkUpdateSeatStatus
}
