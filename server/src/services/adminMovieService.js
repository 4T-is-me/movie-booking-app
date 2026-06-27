import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';

export const createMovie = async (movieData) => {
    const newMovie = new Movie(movieData);
    return await newMovie.save();
};

export const updateMovie = async (movieId, updateData) => {
    return await Movie.findByIdAndUpdate(movieId, updateData, { new: true });
};

export const deleteMovie = async (movieId) => {
    return await Movie.findByIdAndDelete(movieId);
};

/**
 * GÁN PHIM VÀO RẠP CHIẾU
 * Admin chỉ định phim được chiếu ở những rạp nào.
 * Các thông tin như roomId, showDate, startTime, endTime sẽ là null.
 */
export const assignMovieToCinemas = async (movieId, cinemaIds) => {
    const movie = await Movie.findById(movieId);
    if (!movie) throw new Error('MovieNotFound');

    // Tạo các bản ghi showtime "nháp" chỉ chứa movieId và cinemaId
    const pendingShowtimes = cinemaIds.map(cinemaId => ({
        movieId: movieId,
        cinemaId: cinemaId,
        roomId: null,
        showDate: null,
        showTime: null,
        startTime: null,
        endTime: null
    }));

    // Lưu hàng loạt vào DB
    const assignedCinemas = await Showtime.insertMany(pendingShowtimes);
    return assignedCinemas;
};
