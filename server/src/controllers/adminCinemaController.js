import { adminCinemaService } from "../services/adminCinemaService.js";
import { StatusCodes } from "http-status-codes";

const PHONE_REGEX = /^[0-9\-\+\s]{8,15}$/;

// POST /api/admin/cinemas
const createCinema = async (req, res, next) => {
    let { cinemaName, address, city, hotline, image, status } = req.body;
    try {
        if (!cinemaName || !address || !city || !hotline) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Missing required fields: cinemaName, address, city, hotline"
            });
        }

        if (!PHONE_REGEX.test(hotline.trim())) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Invalid phone number format."
            });
        }

        const result = await adminCinemaService.handleCreateCinema({
            cinemaName: cinemaName.trim(),
            address: address.trim(),
            city: city.trim(),
            hotline: hotline.trim(),
            image: image || "",
            status: status || "active"
        });

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/cinemas
const getAllCinemas = async (req, res, next) => {
    try {
        const cinemas = await adminCinemaService.handleGetAllCinemas();
        return res.status(StatusCodes.OK).json({ success: true, data: cinemas });
    } catch (error) {
        next(error);
    }
};

const getCinemaDetails = async (req, res, next) => {
    try {
        const { id } = req.params;
        const cinema = await adminCinemaService.handleGetCinemaDetails(id);
        if (!cinema) {
            return res.status(StatusCodes.NOT_FOUND).json({ success: false, message: "Cinema not found" });
        }
        return res.status(StatusCodes.OK).json({ success: true, data: cinema });
    } catch (error) {
        next(error);
    }
};

const updateCinema = async (req, res, next) => {
    try {
        const { id } = req.params;
        let { cinemaName, address, city, hotline, image, status } = req.body;

        if (hotline && !PHONE_REGEX.test(hotline.trim())) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: "Invalid phone number format."
            });
        }

        const updateData = {
            cinemaName,
            address,
            city,
            hotline,
            status
        };
        if (image) {
            updateData.image = image;
        }

        const result = await adminCinemaService.handleUpdateCinema(id, updateData);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const toggleCinemaStatus = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { status } = req.body;
        if (!status || (status !== "active" && status !== "inactive")) {
            return res.status(StatusCodes.BAD_REQUEST).json({ success: false, message: "Invalid status: must be active or inactive" });
        }
        const result = await adminCinemaService.handleToggleCinemaStatus(id, status);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

export const adminCinemaController = {
    createCinema,
    getAllCinemas,
    getCinemaDetails,
    updateCinema,
    toggleCinemaStatus
};
