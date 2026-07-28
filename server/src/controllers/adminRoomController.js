import {adminRoomService} from "../services/adminRoomService.js";
import { StatusCodes } from "http-status-codes";
import { Cinema } from "../models/cinemaModel.js";


//POST /api/admin/rooms - create room
const creatRoomWithSeats = async (req, res, next) => {
    let { cinemaId, roomName, roomType, screenType, rows, columns, rowsCount, colsCount, status, seats } = req.body;
    try {
        const finalRows = rows || rowsCount;
        const finalCols = columns || colsCount;

        if (!roomName || !roomType || finalRows === undefined || finalCols === undefined) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Missing required fields: roomName, roomType, rows (or rowsCount), columns (or colsCount)"
            });
        }

        const numRows = Number(finalRows);
        const numCols = Number(finalCols);

        if (isNaN(numRows) || numRows < 1 || numRows > 8) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Rows must be between 1 and 8."
            });
        }

        if (isNaN(numCols) || numCols < 1 || numCols > 10) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Columns must be between 1 and 10."
            });
        }

        let targetCinemaId = cinemaId;
        if (!targetCinemaId) {
            const defaultCinema = await Cinema.findOne({});
            if (!defaultCinema) {
                return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({
                    success: false,
                    message: "No cinemas found in database to associate room with."
                });
            }
            targetCinemaId = defaultCinema._id;
        }

        const result = await adminRoomService.handleCreateRoom({
            cinemaId: targetCinemaId,
            roomName: roomName.trim(),
            roomType,
            screenType: screenType || "2D",
            rowsCount: Number(finalRows),
            colsCount: Number(finalCols),
            status: status || "active",
            seatsInput: seats
        });

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: result
        });
        
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}
//GET /api/admin/rooms get all rooms(admin)
const getAllRooms = async (req, res, next) => {
    try {
        let cinemaId = req.query.cinemaId;

        const rooms = await adminRoomService.handleGetAllRoom(cinemaId);
        return res.status(StatusCodes.OK).json({success: true, data: rooms})
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

//GET /api/admin/rooms/:id/seats - get seats of a room
const getRoomSeats = async (req, res, next) => {
    try {
        const roomId = req.params.id;
        const seatsMatrix = await adminRoomService.handleGetRoomSeats(roomId);
        return res.status(StatusCodes.OK).json({success: true, data: seatsMatrix})
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

//PATCH /api/admin/rooms/:id/seats - update seat types of a room
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

//DELETE /api/admin/rooms/:id - delete a room
const softDeleteRoom = async (req, res, next) => {
    try {
        const {id: roomId} = req.params;
        const result = await adminRoomService.handleSoftDeleteRoom(roomId);
        return res.status(StatusCodes.OK).json({success: true, data: result})
        
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
}

const getRoomDetails = async (req, res, next) => {
    try {
        const { id } = req.params;
        const room = await adminRoomService.handleGetRoomDetails(id);
        if (!room) {
            return res.status(StatusCodes.NOT_FOUND).json({ success: false, message: "Room not found" });
        }
        return res.status(StatusCodes.OK).json({ success: true, data: room });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
};

const updateRoom = async (req, res, next) => {
    try {
        const { id } = req.params;
        let { roomName, roomType, screenType, rows, columns, rowsCount, colsCount, status, seats } = req.body;

        const finalRows = rows || rowsCount;
        const finalCols = columns || colsCount;

        if (finalRows !== undefined) {
            const numRows = Number(finalRows);
            if (isNaN(numRows) || numRows < 1 || numRows > 8) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: "Rows must be between 1 and 8."
                });
            }
        }

        if (finalCols !== undefined) {
            const numCols = Number(finalCols);
            if (isNaN(numCols) || numCols < 1 || numCols > 10) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: "Columns must be between 1 and 10."
                });
            }
        }

        const result = await adminRoomService.handleUpdateRoom(id, {
            roomName,
            roomType,
            screenType,
            rowsCount: finalRows ? Number(finalRows) : undefined,
            colsCount: finalCols ? Number(finalCols) : undefined,
            status,
            seatsInput: seats
        });

        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        if (error.statusCode) {
            return res.status(error.statusCode).json({ success: false, message: error.message });
        }
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
};

const toggleRoomStatus = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { status } = req.body;
        if (!status || (status !== "active" && status !== "inactive")) {
            return res.status(StatusCodes.BAD_REQUEST).json({ success: false, message: "Invalid status: must be active or inactive" });
        }
        const result = await adminRoomService.handleToggleRoomStatus(id, status);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ success: false, message: `Internal server error: ${error.message}` });
    }
};

export const adminRoomController = {
    creatRoomWithSeats,
    getAllRooms,
    getRoomSeats,
    updateSeatTypes,
    softDeleteRoom,
    getRoomDetails,
    updateRoom,
    toggleRoomStatus
}