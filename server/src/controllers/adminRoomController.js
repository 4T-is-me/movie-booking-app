import {adminRoomService} from "../services/adminRoomService.js";
import { StatusCodes } from "http-status-codes";


//create room
const creatRoomWithSeats = async (req, res, next) => {
    const {cinemaId, roomName, roomType, rowsCount, colsCount} = req.body;
    try {
        if (!cinemaId || !roomName || !roomType || !rowsCount || !colsCount) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Missing required fields: cinemaId, roomName, roomType, rowsCount, colsCount"
            });
        }
        const result = await adminRoomService.handleCreateRoom({
            cinemaId,
            roomName: roomName.trim(),
            roomType,
            rowsCount: Number(rowsCount),
            colsCount: Number(colsCount)
        });

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: result
        });
        
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}
// get all rooms(admin)
const getAllRooms = async (req, res, next) => {
    try {
        const rooms = await adminRoomService.handleGetAllRoom();
        return res.status(StatusCodes.OK).json({success: true, data: rooms})
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}
// get seats of a room
const getRoomSeats = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const seatsMatrix = await adminRoomService.handleGetRoomSeats(roomId);
        return res.status(StatusCodes.OK).json({success: true, data: seatsMatrix})
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

// update seat types
const updateSeatTypes = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const updateData = req.body;
        const result = await adminRoomService.handleUpdateSeatTypes(roomId, updateData);
        return res.status(StatusCodes.OK).json({success: true, data: result})
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}
// soft delete
const softDeleteRoom = async (req, res, next) => {
    try {
        const {id: roomId} = req.params;
        const result = await adminRoomService.handleSoftDeleteRoom(roomId);
        return res.status(StatusCodes.OK).json({success: true, data: result})
        
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

export const adminRoomController = {
    creatRoomWithSeats,
    getAllRooms,
    getRoomSeats,
    updateSeatTypes,
    softDeleteRoom
}