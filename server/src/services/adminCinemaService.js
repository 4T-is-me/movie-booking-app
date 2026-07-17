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
        const cinemas = await Cinema.find({});
        return cinemas;
    } catch (error) {
        throw error;
    }
};

export const adminCinemaService = {
    handleCreateCinema,
    handleGetAllCinemas
};
