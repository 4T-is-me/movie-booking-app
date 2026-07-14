import ApiError from "../utils/ApiError.js";
import { CinemaRoom } from "../models/cinemaRoomModel.js";
import { Seat } from "../models/seatModel.js";
import mongoose from "mongoose";
import { StatusCodes } from "http-status-codes";

const handleGetManagerRooms = async (cinemaId) => {
    try {
        const matchStage = {
            isDeleted: false,
            cinemaId: new mongoose.Types.ObjectId(cinemaId)
        };

        const rooms = await CinemaRoom.aggregate([
            { $match: matchStage },
            {
                $lookup: {
                    from: 'cinemas', 
                    localField: 'cinemaId',
                    foreignField: '_id',
                    as: 'cinemaInfo'
                }
            },
            {
                $unwind: {
                    path: '$cinemaInfo',
                    preserveNullAndEmptyArrays: true 
                }
            },
            {
                $lookup: {
                    from: 'seats',
                    localField: '_id',
                    foreignField: 'roomId',
                    as: 'seats'
                }
            },
            {
                $unwind: {
                    path: '$seats',
                    preserveNullAndEmptyArrays: true
                }
            },
            {
                $group: {
                    _id: '$_id',
                    roomName: {$first: '$roomName'},
                    roomType: {$first: '$roomType'},
                    status: {$first: '$status'},
                    rowsCount: {$first: '$rowsCount'},
                    colsCount: {$first: '$colsCount'},
                    cinemaName: {$first: '$cinemaInfo.cinemaName'},
                    totalSeats: {
                        $sum: {$cond: [{ $ifNull: ['$seats', false]}, 1, 0]} 
                    },
                    vipSeats: {
                        $sum: { $cond: [ { $eq: ['$seats.seatType', 'VIP'] }, 1, 0 ] }
                    },
                    regularSeats: {
                        $sum: { $cond: [ { $eq: ['$seats.seatType', 'Regular'] }, 1, 0 ] }
                    },
                    coupleSeats: {
                        $sum: { $cond: [ { $eq: ['$seats.seatType', 'Couple'] }, 1, 0 ] }
                    },
                    regularPrice: {
                        $max: { $cond: [ { $eq: ['$seats.seatType', 'Regular'] }, '$seats.price', 0 ] }
                    },
                    vipPrice: {
                        $max: { $cond: [ { $eq: ['$seats.seatType', 'VIP'] }, '$seats.price', 0 ] }
                    },
                    couplePrice: {
                        $max: { $cond: [ { $eq: ['$seats.seatType', 'Couple'] }, '$seats.price', 0 ] }
                    },
                    estFullCapacity: {
                        $sum: '$seats.price'
                    }
                }
            },
            { $sort: { roomName: 1 } }
        ]);

        return rooms;
    } catch (error) {
        throw new ApiError(StatusCodes.INTERNAL_SERVER_ERROR, error.message);
    }
}

const handleGetRoomSeats = async (roomId, cinemaId) => {
    const room = await CinemaRoom.findOne({
        _id: roomId, 
        cinemaId: cinemaId,
        isDeleted: false
    });
    if(!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found or does not belong to your cinema");
    }
    const seats = await Seat.find({roomId: roomId, status: "active", isDeleted: false}).sort({row: 1, col: 1});
    
    const groupObject = seats.reduce((acc, seat) => {
        if(!acc[seat.row]) {
            acc[seat.row] = { row: seat.row, seats: [] };
        }
        acc[seat.row].seats.push({
            _id: seat._id,
            seatName: seat.seatName,
            seatType: seat.seatType,
            row: seat.row,
            col: seat.col,
            status: seat.status 
        });
        return acc;
    }, {});
    
    return Object.values(groupObject);
}

const handleUpdateRoom = async (roomId, cinemaId, data) => {
    const room = await CinemaRoom.findOne({
        _id: roomId,
        cinemaId: cinemaId,
        isDeleted: false
    });
    if(!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found or does not belong to your cinema");
    }

    if (data.status && room.status !== data.status) {
        const oldStatus = room.status;
        room.status = data.status;
        await room.save();
        
        // Trigger maintenance workflow if status changed to maintenance
        if (data.status === 'maintenance') {
            try {
                const { MaintenanceService } = await import('./maintenanceService.js');
                await MaintenanceService.handleRoomMaintenance(roomId);
            } catch (err) {
                console.error("Error running maintenance workflow:", err);
            }
        }
    }

    if (data.regularPrice !== undefined) {
        await Seat.updateMany(
            { roomId: roomId, seatType: 'Regular', isDeleted: false },
            { $set: { price: data.regularPrice } }
        );
    }
    if (data.vipPrice !== undefined) {
        await Seat.updateMany(
            { roomId: roomId, seatType: 'VIP', isDeleted: false },
            { $set: { price: data.vipPrice } }
        );
    }
    if (data.couplePrice !== undefined) {
        await Seat.updateMany(
            { roomId: roomId, seatType: 'Couple', isDeleted: false },
            { $set: { price: data.couplePrice } }
        );
    }

    return room;
}

export const managerRoomService = {
    handleGetManagerRooms,
    handleGetRoomSeats,
    handleUpdateRoom
};
