import * as adminMovieService from '../services/adminMovieService.js';

export const createMovie = async (req, res) => {
    try {
        const { movieName, duration, releaseDate, movieGenre, language, censorship, director, actors, country } = req.body;
        
        // Validate cơ bản
        if (!movieName || !duration || !releaseDate) {
            return res.status(400).json({ success: false, message: 'Vui lòng điền đủ các trường bắt buộc.' });
        }

        const newMovie = await adminMovieService.createMovie(req.body);
        return res.status(201).json({ success: true, data: newMovie, message: 'Thêm phim thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const updateMovie = async (req, res) => {
    try {
        const { id } = req.params;
        const updatedMovie = await adminMovieService.updateMovie(id, req.body);
        
        if (!updatedMovie) return res.status(404).json({ success: false, message: 'Không tìm thấy phim.' });
        
        return res.status(200).json({ success: true, data: updatedMovie, message: 'Cập nhật phim thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const deleteMovie = async (req, res) => {
    try {
        const { id } = req.params;
        const deletedMovie = await adminMovieService.deleteMovie(id);
        
        if (!deletedMovie) return res.status(404).json({ success: false, message: 'Không tìm thấy phim.' });
        
        return res.status(200).json({ success: true, message: 'Xóa phim thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const assignMovieToCinemas = async (req, res) => {
    try {
        const { id } = req.params; // movieId
        const { cinemaIds } = req.body; // Mảng các ID rạp: ["id_rap_1", "id_rap_2"]
        
        if (!cinemaIds || !Array.isArray(cinemaIds) || cinemaIds.length === 0) {
            return res.status(400).json({ success: false, message: 'Danh sách rạp chiếu không hợp lệ.' });
        }

        const result = await adminMovieService.assignMovieToCinemas(id, cinemaIds);
        return res.status(201).json({ 
            success: true, 
            data: result, 
            message: 'Đã gán phim vào rạp thành công. Chờ quản lý rạp xếp lịch.' 
        });
    } catch (error) {
        if (error.message === 'MovieNotFound') {
            return res.status(404).json({ success: false, message: 'Phim không tồn tại.' });
        }
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};
