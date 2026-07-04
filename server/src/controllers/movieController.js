import {
    getMovies,
    searchMovies,
    getMovieById,
    getMovieShowtimes
} from '../services/movieService.js';

import { StatusCodes } from 'http-status-codes';

// GET /api/movies
export const handleGetMovies = async (req, res) => {
    try {
        const { status, page, limit } = req.query;
        const result = await getMovies({ status, page, limit });
        return res.status(StatusCodes.OK).json(result);
    } catch (error) {
        console.error('handleGetMovies error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Internal server error.' });
    }
};

// GET /api/movies/search?q=...
export const handleSearchMovies = async (req, res) => {
    try {
        const { q } = req.query;
        const result = await searchMovies({ q });
        return res.status(StatusCodes.OK).json(result);
    } catch (error) {
        console.error('handleSearchMovies error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Internal server error.' });
    }
};

// GET /api/movies/:id
export const handleGetMovieById = async (req, res) => {
    try {
        const { id } = req.params;
        const movie = await getMovieById(id);
        if (!movie) {
            return res.status(StatusCodes.NOT_FOUND).json({ message: 'Movie not found.' });
        }
        return res.status(StatusCodes.OK).json({ data: movie });
    } catch (error) {
        console.error('handleGetMovieById error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Internal server error.' });
    }
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
// Returns the list of showtimes for that day including cinemaRoomName
export const handleGetMovieShowtimes = async (req, res) => {
    try {
        const { id } = req.params;
        const { date } = req.query;
        const result = await getMovieShowtimes(id, date);
        if (!result) {
            return res.status(404).json({
                message: 'No showtimes found for this movie on the given date.',
                movieId: id,
                date: date || null
            });
        }
        return res.status(StatusCodes.OK).json({ data: result });
    } catch (error) {
        console.error('handleGetMovieShowtimes error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Internal server error.' });
    }
};
