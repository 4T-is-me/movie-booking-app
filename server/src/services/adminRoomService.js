import ApiError from "../utils/ApiError.js";
import { Cinema } from "../models/cinemaModel.js";
import { CinemaRoom } from "../models/cinemaRoomModel.js";
import { Seat } from "../models/seatModel.js";
import mongoose from "mongoose";
import { StatusCodes } from "http-status-codes";
import { Showtime } from "../models/showTimeModel.js";

//create room and generate seats
const handleCreateRoom = async ({ cinemaId, roomName, roomType, screenType, rowsCount, colsCount, status, seatsInput }) => {
    const session = await mongoose.startSession();

    try {
        session.startTransaction();

        //check cinema exists
        const cinema = await Cinema.findById(cinemaId).session(session);
        if (!cinema) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Cinema not found!");
        }

        //check room exists
        const room = await CinemaRoom.findOne({
            cinemaId,
            roomName,
            isDeleted: false
        }).session(session);

        if (room) {
            throw new ApiError(StatusCodes.CONFLICT, "Room name already exists!");
        }

        //validate screen type
        const VALID_SCREEN_TYPES = ["2D", "3D", "IMAX"];
        const finalScreenType = (screenType || "2D").toUpperCase();

        if (!VALID_SCREEN_TYPES.includes(finalScreenType)) {
            throw new ApiError(
                StatusCodes.BAD_REQUEST,
                "Invalid screen type! It must be 2D, 3D or IMAX!"
            );
        }
        //total seats
        const totalSeats = rowsCount * colsCount;

        //create room
        const [createdRoom] = await CinemaRoom.create(
            [
                {
                    cinemaId,
                    roomName,
                    rowsCount,
                    colsCount,
                    totalSeats,
                    roomType: roomType || 'Standard',
                    screenType: finalScreenType,
                    status: status || 'active'
                }
            ],
            { session }
        );

        //generate seats
        const seats = [];

        if (seatsInput && Array.isArray(seatsInput) && seatsInput.length > 0) {
            for (const s of seatsInput) {
                const name = s.seatName; // e.g. "A1"
                const inputType = s.seatType || "standard";

                // Parse row and col from seatName
                const rowLetter = name.charAt(0);
                const colNumber = parseInt(name.substring(1), 10);

                let seatType = "Regular";
                let price = 100000;

                if (inputType.toLowerCase() === "vip") {
                    seatType = "VIP";
                    price = 150000;
                } else if (inputType.toLowerCase() === "couple") {
                    seatType = "Couple";
                    price = 250000;
                }

                seats.push({
                    roomId: createdRoom._id,
                    seatName: name,
                    row: rowLetter,
                    col: colNumber,
                    price,
                    status: "active",
                    seatType
                });
            }
        } else {
            for (let row = 0; row < rowsCount; row++) {
                //convert row to letter
                const rowLetter = String.fromCharCode(row + 65);

                for (let col = 1; col <= colsCount; col++) {
                    //default
                    let seatType = "Regular";
                    let price = 100000;

                    // the last row -> Couple
                    if (row === rowsCount - 1) {
                        seatType = "Couple";
                        price = 250000;
                    }
                    // the last 2 rows -> VIP
                    else if (row === rowsCount - 2 || row === rowsCount - 3) {
                        seatType = "VIP";
                        price = 150000;
                    }

                    seats.push({
                        roomId: createdRoom._id,
                        seatName: `${rowLetter}${col}`,
                        row: rowLetter,
                        col,
                        price,
                        status: "active",
                        seatType
                    });
                }
            }
        }

        //insert many
        await Seat.insertMany(seats, { session });

        await session.commitTransaction();

        return {
            room: createdRoom,
            seats
        };
    } catch (error) {
        await session.abortTransaction();
        throw error;
    } finally {
        session.endSession();
    }
};

const handleGetAllRoom = async (cinemaId) => {
    try {
        const matchStage = {
            isDeleted: false
        };
        if (cinemaId) {
            matchStage.cinemaId = new mongoose.Types.ObjectId(cinemaId);
        }

        const rooms = await CinemaRoom.aggregate([
            // Filter rooms that have not been deleted.
            {
                $match: matchStage
            },
            // Join with the Cinema table(CinemaRoom join Cinema) 
            // from: name collection, cinemaId: CinemaRoom table, _id: Cinema table
            {
                $lookup: {
                    from: 'cinemas',
                    localField: 'cinemaId',
                    foreignField: '_id',
                    as: 'cinemaInfo'
                }
            },
            // Extract element from the array
            {
                $unwind: {
                    path: '$cinemaInfo',
                    preserveNullAndEmptyArrays: true
                }
            },

            // Join with the Seat table(CinemaRoom join Seat)
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
            //Group the types of chairs together - first: get the first element
            {
                $group: {
                    _id: '$_id',
                    roomName: { $first: '$roomName' },
                    roomType: { $first: '$roomType' },
                    screenType: { $first: '$screenType' },
                    status: { $first: '$status' },
                    rowsCount: { $first: '$rowsCount' },
                    colsCount: { $first: '$colsCount' },
                    cinemaName: { $first: '$cinemaInfo.cinemaName' },
                    totalSeats: {
                        //$cond: [{ if : [true, false]}, 1, 0]
                        $sum: { $cond: [{ $ifNull: ['$seats', false] }, 1, 0] }
                    },
                    // count VIP 
                    vipSeats: {
                        $sum: {
                            $cond: [
                                { $eq: ['$seats.seatType', 'VIP'] }, 1, 0
                            ]
                        }
                    },
                    // count Regular
                    regularSeats: {
                        $sum: {
                            $cond: [
                                { $eq: ['$seats.seatType', 'Regular'] }, 1, 0
                            ]
                        }
                    },
                    // count Couple
                    coupleSeats: {
                        $sum: {
                            $cond: [
                                { $eq: ['$seats.seatType', 'Couple'] }, 1, 0
                            ]
                        }
                    }
                }
            },
            {
                // Sort by room name
                $sort: { roomName: 1 }
            }
        ])

        return rooms

    } catch (error) {
        throw new ApiError(StatusCodes.INTERNAL_SERVER_ERROR, error.message);
    }
}
const handleGetRoomSeats = async (roomId) => {
    //check room exists
    const room = await CinemaRoom.findOne({ _id: roomId, isDeleted: false, status: "active" });
    if (!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found");
    }
    //get all Seat of a Room + sort by row and col
    const seats = await Seat.find({ roomId: roomId, status: "active", isDeleted: false }).sort({ row: 1, col: 1 });

    const groupObject = seats.reduce((acc, seat) => {
        // Create new row if it doest not exist
        if (!acc[seat.row]) {
            acc[seat.row] = {
                row: seat.row,
                seats: []
            };
        }
        // Push seat in the same row
        acc[seat.row].seats.push({
            _id: seat._id,
            seatName: seat.seatName, // VD: "A1", "A2"
            seatType: seat.seatType, // VD: "Regular", "VIP", "Couple"
            row: seat.row,
            col: seat.col,
            status: seat.status
        });
        return acc;
    }, {});
    //Convert object to array
    const finalResult = Object.values(groupObject);
    return finalResult;
}

const handleUpdateSeatTypes = async (roomId, seatType) => {
    //check room exists
    const room = await CinemaRoom.findOne({ _id: roomId, isDeleted: false, status: "active" });
    if (!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found");
    }
    //Validate body
    if (!Array.isArray(seatType) || seatType.length === 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "The updated seat list must not be left blank!");
    }
    //type seat :
    const allowSeatType = ['Regular', 'VIP', 'Couple'];

    for (const item of seatType) {
        //check seatId
        if (!item.seatId) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "Invalid seat type!");
        }
        //check seatType
        if (!allowSeatType.includes(item.seatType)) {
            throw new ApiError(StatusCodes.BAD_REQUEST, `Seat type "${item.seatType}" is invalid! Only the following are accepted: ${allowedSeatTypes.join(', ')}`);
        }
    }
    //Create operations array for bulkWrite 
    // Enforce a filter matching the roomId => prevent accidentally updating a different room
    const operations = seatType.map(item => {
        return {
            updateOne: {
                //filter(seat + roomId) + updates(seatType)
                filter: { _id: item.seatId, roomId: roomId },
                update: { $set: { seatType: item.seatType } }
            }
        }
    })
    //Execute bulkWrite to MongoDB (only 1 request to the database)
    const bulkWriteResult = await Seat.bulkWrite(operations);
    return bulkWriteResult;
}

const handleSoftDeleteRoom = async (roomId) => {
    const room = await CinemaRoom.findOne({ _id: roomId, isDeleted: false, status: "active" });
    if (!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found");
    }
    //to day
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    // Check if room has any scheduled screenings from today onward
    const futureShowtime = await Showtime.findOne({ roomId: roomId, showDate: { $gte: today }, isDeleted: false });

    //if has
    if (futureShowtime) {
        throw new ApiError(StatusCodes.CONFLICT, "This room cannot be deleted because there are scheduled screenings in the future!");
    }

    room.isDeleted = true;
    const result = await room.save();
    return result
}

const handleToggleRoomStatus = async (roomId, status) => {
    const room = await CinemaRoom.findOne({ _id: roomId, isDeleted: false });
    if (!room) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Room not found!");
    }
    if (status !== 'active' && status !== 'inactive') {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Invalid status! Must be 'active' or 'inactive'.");
    }
    room.status = status;
    const result = await room.save();
    return result;
}

export const adminRoomService = {
    handleCreateRoom,
    handleGetAllRoom,
    handleGetRoomSeats,
    handleUpdateSeatTypes,
    handleSoftDeleteRoom,
    handleToggleRoomStatus
};