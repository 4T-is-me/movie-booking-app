import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';
//create a new movie
const createMovie = async (movieData) => {
    // check the same movieName exists or not
    const existingMovie = await Movie.findOne({ movieName: movieData.movieName, isDeleted: false });
    if (existingMovie) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'This movie title already exists in the system!');
    }

    const newMovie = await Movie.create(movieData);
    return newMovie;
};
// update movie
const updateMovie = async (movieId, updateData) => {
    const movie = await Movie.findOne({ _id: movieId, isDeleted: false });
    if (!movie) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found or has been deleted!');
    }

    // update
    const updatedMovie = await Movie.findByIdAndUpdate(
        movieId,
        { $set: updateData },
        { new: true, runValidators: true }
    );

    return updatedMovie;
};

// soft delete movie
const softDeleteMovie = async (movieId) => {
    const movie = await Movie.findOne({ _id: movieId, isDeleted: false });
    if (!movie) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found or has been deleted!');
    }

    // Update status to isDeleted: true and set deletedAt timestamp
    const deletedMovie = await Movie.findByIdAndUpdate(
        movieId,
        { 
            $set: { 
                isDeleted: true,
                deletedAt: new Date()
            } 
        },
        { new: true }
    );

    return {
        message: 'Soft delete movie successful!',
        id: deletedMovie._id
    };
};

/**
 * ASSIGN MOVIE TO CINEMAS
 * Admin specifies which cinemas the movie will be screened at.
 * Information such as roomId, showDate, startTime, endTime will be null.
 */
const assignMovieToCinemas = async (movieId, cinemaIds) => {
    const movie = await Movie.findById(movieId);
    if (!movie) throw new Error('MovieNotFound');

    // Create draft showtime records containing only movieId and cinemaId
    const pendingShowtimes = cinemaIds.map(cinemaId => ({
        movieId: movieId,
        cinemaId: cinemaId,
        roomId: null,
        showDate: null,
        showTime: null,
        startTime: null,
        endTime: null
    }));

    // Batch insert into DB
    const assignedCinemas = await Showtime.insertMany(pendingShowtimes);
    return assignedCinemas;
};

const getMoviesAdmin = async (query) => {
    const { status, page = 1, limit = 10, search } = query;
    const parsedPage = parseInt(page, 10);
    const parsedLimit = parseInt(limit, 10);
    
    const skip = (parsedPage - 1) * parsedLimit;
    const q = {};
    if(search) {
        q.movieName = { $regex: search, $options: 'i' };
    }

    if(status == 'deleted') {
        // Retrieve only soft-deleted movies
        q.isDeleted = true;
    }else {
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

export const adminMovieService = {
    createMovie,
    updateMovie,
    softDeleteMovie,
    assignMovieToCinemas,
    getMoviesAdmin
};