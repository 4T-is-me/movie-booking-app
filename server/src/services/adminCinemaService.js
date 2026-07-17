import { Cinema } from "../models/cinemaModel.js";

const handleCreateCinema = async (cinemaData) => {
    try {
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
            throw new Error("Cinema not found");
        }
        if (updateData.cinemaName) cinema.cinemaName = updateData.cinemaName.trim();
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
            throw new Error("Cinema not found");
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
