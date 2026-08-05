import { Movie } from '../models/movieModel.js';
import { Showtime } from '../models/showTimeModel.js';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';
//create a new movie
const createMovie = async (movieData) => {
    const existingMovie = await Movie.findOne({
        movieName: movieData.movieName,
        isDeleted: false
    });

    if (existingMovie) {
        throw new ApiError(
            StatusCodes.BAD_REQUEST,
            'This movie title already exists in the system!'
        );
    }

    const today = new Date();

    // Parse and validate dates safely
    const releaseDate = movieData.releaseDate ? new Date(movieData.releaseDate) : null;
    const endDate = movieData.endDate ? new Date(movieData.endDate) : null;

    if (!releaseDate || Number.isNaN(releaseDate.getTime())) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Invalid or missing releaseDate');
    }

    if (endDate && Number.isNaN(endDate.getTime())) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Invalid endDate');
    }

    if (endDate && endDate <= releaseDate) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'endDate must be after releaseDate');
    }

    // Normalize the date fields in the payload so mongoose stores proper Date objects
    movieData.releaseDate = releaseDate;
    movieData.endDate = endDate || null;

    // Determine status based on dates
    if (today < releaseDate) {
        movieData.status = 'coming_soon';
    } else if (endDate && today > endDate) {
        movieData.status = 'ended';
    } else {
        movieData.status = 'showing';
    }

    return await Movie.create(movieData);
};
    // update movie
    const updateMovie = async (movieId, updateData) => {
        const movie = await Movie.findById(movieId);
        if (!movie) {
            throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found!');
        }

        const updatePayload = { ...updateData };
        if (updatePayload.isDeleted === undefined) {
            updatePayload.isDeleted = movie.isDeleted ?? true;
        }

        const updatedMovie = await Movie.findByIdAndUpdate(
            movieId,
            { $set: updatePayload },
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

    const getMoviesAdmin = async (query = {}) => {
        const { status, page = 1, limit = 10, search } = query;
        const parsedPage = parseInt(page, 10);
        const parsedLimit = parseInt(limit, 10);

        const skip = (parsedPage - 1) * parsedLimit;
        const q = {};
        if (search) {
            q.movieName = { $regex: search, $options: 'i' };
        }

        if (status === 'deleted' || status === 'inactive') {
            q.isDeleted = true;
        } else if (status === 'active') {
            q.isDeleted = false;
        } else if (status === 'showing') {
            q.status = 'showing';
        } else if (status === 'coming-soon' || status === 'coming_soon') {
            q.status = 'coming_soon';
        }

        const [totalCount, movies] = await Promise.all([
            Movie.countDocuments(q),
            Movie.find(q).skip(skip).limit(parsedLimit).sort({ createdAt: -1 }).lean()
        ]);
        const totalPages = Math.ceil(totalCount / parsedLimit);
        return {
            page: parsedPage,
            limit: parsedLimit,
            total: totalCount,
            totalPages,
            data: movies
        }
    }

export const adminMovieService = {
    createMovie,
    updateMovie,
    softDeleteMovie,
    assignMovieToCinemas,
    getMoviesAdmin
};

