import { adminMovieService } from '../services/adminMovieService.js';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';

// POST /api/admin/movies - 
const createMovie = async (req, res, next) => {
    try {
        const newMovie = await adminMovieService.createMovie(req.body);
        console.log(">>> CHECK REQ.BODY TRONG CONTROLLER:", req.body);
        return res.status(StatusCodes.CREATED).json({ 
            success: true, 
            data: newMovie, 
            message: 'Thêm phim thành công.' 
        });
    } catch (error) {
        next(error);
    }
};

// PUT /api/admin/movies/:id
const updateMovie = async (req, res, next) => {
    try {
        const { id } = req.params;
        const updatedMovie = await adminMovieService.updateMovie(id, req.body);
        
        if (!updatedMovie) {
            throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found.');
        }
        
        return res.status(StatusCodes.OK).json({ 
            success: true, 
            data: updatedMovie, 
            message: 'Updated movie successfully.' 
        });
    } catch (error) {
        next(error);
    }
};

// DELETE /api/admin/movies/:id
const deleteMovie = async (req, res, next) => {
    try {
        const { id } = req.params;
        await adminMovieService.softDeleteMovie(id); // Gọi hàm soft delete từ service mới viết
        
        return res.status(StatusCodes.OK).json({ 
            success: true, 
            message: 'Deleted movie successfully.' 
        });
    } catch (error) {
        next(error);
    }
};


const assignMovieToCinemas = async (req, res, next) => {
    try {
        const { id } = req.params; 
        const { cinemaIds } = req.body; 
        
        if (!cinemaIds || !Array.isArray(cinemaIds) || cinemaIds.length === 0) {
            throw new ApiError(StatusCodes.BAD_REQUEST, 'Invalid cinema list.');
        }

        const result = await adminMovieService.assignMovieToCinemas(id, cinemaIds);
        
        return res.status(StatusCodes.CREATED).json({ 
            success: true, 
            data: result, 
            message: 'The movie has been successfully assigned to the cinema. Awaiting the cinema manager to schedule the screenings.' 
        });
    } catch (error) {
        next(error);
    }
};

// GET /api/admin/movies
const getMoviesAdmin = async (req, res, next) => {
    try {
        const result = await adminMovieService.getMoviesAdmin(req.query);
        
        return res.status(StatusCodes.OK).json({ 
            success: true, 
            data: result, 
            message: 'Get movies for admin successfully.' 
        });
    } catch (error) {
        next(error);
    }
};

export const adminMovieController = {
    createMovie,
    updateMovie,
    deleteMovie,
    assignMovieToCinemas,
    getMoviesAdmin
};