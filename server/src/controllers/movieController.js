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
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Lỗi máy chủ nội bộ.' });
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
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};

// GET /api/movies/:id
export const handleGetMovieById = async (req, res) => {
    try {
        const { id } = req.params;
        const movie = await getMovieById(id);
        if (!movie) {
            return res.status(StatusCodes.NOT_FOUND).json({ message: 'Không tìm thấy phim.' });
        }
        return res.status(StatusCodes.OK).json({ data: movie });
    } catch (error) {
        console.error('handleGetMovieById error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};

// GET /api/movies/:id/showtimes?date=YYYY-MM-DD
export const handleGetMovieShowtimes = async (req, res) => {
    try {
        const { id } = req.params;
        const { date } = req.query;
        const result = await getMovieShowtimes(id, date);
        if (!result) {
            return res.status(404).json({ message: 'Không tìm thấy ngày chiếu cho phim này.' });
        }
        return res.status(StatusCodes.OK).json({ data: result });
    } catch (error) {
        console.error('handleGetMovieShowtimes error:', error);
        return res.status(StatusCodes.INTERNAL_SERVER_ERROR).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};
