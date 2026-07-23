import { Cinema } from "../models/cinemaModel.js";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from "http-status-codes";

const handleCreateCinema = async (cinemaData) => {
    try {
        const existingCinema = await Cinema.findOne({
            cinemaName: { $regex: new RegExp(`^${cinemaData.cinemaName.trim()}$`, "i") },
            isDeleted: { $ne: true }
        });
        if (existingCinema) {
            throw new ApiError(StatusCodes.CONFLICT, "Cinema already exists.");
        }

        const newCinema = new Cinema(cinemaData);
        await newCinema.save();
        return newCinema;
    } catch (error) {
        throw error;
    }
};

const handleGetAllCinemas = async () => {
    try {
        const cinemas = await Cinema.aggregate([
            {
                $match: { isDeleted: { $ne: true } }
            },
            {
                $lookup: {
                    from: "cinema_rooms",
                    let: { cinemaId: "$_id" },
                    pipeline: [
                        { $match: { $expr: { $and: [ { $eq: ["$cinemaId", "$$cinemaId"] }, { $eq: ["$isDeleted", false] } ] } } }
                    ],
                    as: "rooms"
                }
            },
            {
                $project: {
                    cinemaName: 1,
                    address: 1,
                    city: 1,
                    hotline: 1,
                    image: 1,
                    status: 1,
                    totalRooms: { $size: "$rooms" }
                }
            }
        ]);
        return cinemas;
    } catch (error) {
        throw error;
    }
};

const handleGetCinemaDetails = async (cinemaId) => {
    try {
        const cinema = await Cinema.findById(cinemaId);
        return cinema;
    } catch (error) {
        throw error;
    }
};

const handleUpdateCinema = async (cinemaId, updateData) => {
    try {
        const cinema = await Cinema.findById(cinemaId);
        if (!cinema) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Cinema not found");
        }

        if (updateData.cinemaName) {
            const existingCinema = await Cinema.findOne({
                _id: { $ne: cinemaId },
                cinemaName: { $regex: new RegExp(`^${updateData.cinemaName.trim()}$`, "i") },
                isDeleted: { $ne: true }
            });
            if (existingCinema) {
                throw new ApiError(StatusCodes.CONFLICT, "Cinema already exists.");
            }
            cinema.cinemaName = updateData.cinemaName.trim();
        }

        if (updateData.address) cinema.address = updateData.address.trim();
        if (updateData.city) cinema.city = updateData.city.trim();
        if (updateData.hotline) cinema.hotline = updateData.hotline.trim();
        if (updateData.image !== undefined) cinema.image = updateData.image;
        if (updateData.status) cinema.status = updateData.status;

        await cinema.save();
        return cinema;
    } catch (error) {
        throw error;
    }
};

const handleToggleCinemaStatus = async (cinemaId, status) => {
    try {
        const cinema = await Cinema.findById(cinemaId);
        if (!cinema) {
            throw new ApiError(StatusCodes.NOT_FOUND, "Cinema not found");
        }
        cinema.status = status;
        await cinema.save();
        return cinema;
    } catch (error) {
        throw error;
    }
};

export const adminCinemaService = {
    handleCreateCinema,
    handleGetAllCinemas,
    handleGetCinemaDetails,
    handleUpdateCinema,
    handleToggleCinemaStatus
};
