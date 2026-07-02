import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';

export const createMovie = async (movieData) => {
    const newMovie = new Movie(movieData);
    return await newMovie.save();
};

export const updateMovie = async (movieId, updateData) => {
    const movie = await Movie.findById(movieId);
    if(!movie || movie.isDeleted) {
        throw new Error('MovieNotFound');
    }
    const {_id, safeUpdateData} = updateData;
    Object.assign(movie, safeUpdateData);
    await movie.save();
    return movie;
};

export const deleteMovie = async (movieId) => {
    const movie = await Movie.findById(movieId);
    if(!movie || movie.isDeleted) {
        throw new Error('MovieNotFound');
    }
    movie.isDeleted = true;
    movie.deletedAt = new Date();
    await movie.save();
    return movie;
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

export const getMoviesAdmin = async (query) => {
    const { status, page = 1, limit = 10, search } = query;
    const parsedPage = parseInt(page, 10);
    const parsedLimit = parseInt(limit, 10);
    
    const skip = (parsedPage - 1) * parsedLimit;
    const q = {};
    if(search) {
        q.movieName = { $regex: search, $options: 'i' };
    }

    if(status == 'deleted') {
        // Chỉ lấy các phim đã bị Soft Delete
        q.isDeleted = true;
    }else {
    //Dùng $ne: true để lấy cả những phim chưa có trường isDeleted
       q.isDeleted = { $ne: true };
        if(status === 'showing') {
            q.status = 'showing';
        }else if(status === 'coming-soon') {
            q.status = 'coming-soon';
        }
    }

    const [ totalCount, movies] = await Promise.all([
        Movie.countDocuments(q),
        Movie.find(q).skip(skip).limit(parsedLimit).sort({ createdAt: -1 }).lean()
    ]);
    const totalPages = Math.ceil(totalCount / parsedLimit);
    return {
        data: movies,
        totalCount,
        totalPages,
        currentPage: parsedPage
    }

}  