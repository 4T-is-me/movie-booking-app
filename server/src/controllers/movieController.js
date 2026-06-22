import {
    getMovies,
    searchMovies,
    getMovieById,
    getMovieShowtimes
} from '../services/movieService.js';

// GET /api/movies - Lấy danh sách phim (lọc status, tìm kiếm q, phân trang page/limit)
export const handleGetMovies = async (req, res) => {
    try {
        const { status, page, limit, q } = req.query;
        const result = await getMovies({ status, page, limit, q });
        return res.status(200).json(result);
    } catch (error) {
        console.error('handleGetMovies error:', error);
        return res.status(500).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};

// GET /api/movies/search?q=...
export const handleSearchMovies = async (req, res) => {
    try {
        const { q } = req.query;
        const result = await searchMovies({ q });
        return res.status(200).json(result);
    } catch (error) {
        console.error('handleSearchMovies error:', error);
        return res.status(500).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};

// GET /api/movies/:id
export const handleGetMovieById = async (req, res) => {
    try {
        const { id } = req.params;
        const movie = await getMovieById(id);
        if (!movie) {
            return res.status(404).json({ message: 'Không tìm thấy phim.' });
        }
        return res.status(200).json({ data: movie });
    } catch (error) {
        console.error('handleGetMovieById error:', error);
        return res.status(500).json({ message: 'Lỗi máy chủ nội bộ.' });
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
        return res.status(200).json({ data: result });
    } catch (error) {
        console.error('handleGetMovieShowtimes error:', error);
        return res.status(500).json({ message: 'Lỗi máy chủ nội bộ.' });
    }
};
