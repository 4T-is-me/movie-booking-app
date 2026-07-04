import Joi from 'joi';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';

const movieRules = {
    movieName: Joi.string().trim().strict(),
    duration: Joi.number().integer().min(1),
    releaseDate: Joi.date(),
    endDate: Joi.date().greater(Joi.ref('releaseDate')).allow(null), // endDate must be after release date
    movieGenre: Joi.array().items(Joi.string().trim().strict()).min(1),
    language: Joi.string().trim().strict(),
    censorship: Joi.string().trim().strict(),
    description: Joi.string().trim().strict().allow('', null),
    director: Joi.array().items(Joi.string().trim().strict()).min(1),
    actors: Joi.array().items(Joi.string().trim().strict()).min(1),
    country: Joi.string().trim().strict(),
    status: Joi.string().valid('coming-soon', 'showing', 'ended').trim().strict(), // Depending on your project status
    poster: Joi.string().trim().strict().allow('', null),
    rating: Joi.number().min(0).max(10),
    totalReviews: Joi.number().min(0),
    totalStarts: Joi.number().min(0)
};

//  Validator for POST /api/admin/movies
const createMovie = async (req, res, next) => {
    const correctCondition = Joi.object({
        movieName: movieRules.movieName.required(),
        duration: movieRules.duration.required(),
        releaseDate: movieRules.releaseDate.required(),
        endDate: movieRules.endDate.optional(),
        movieGenre: movieRules.movieGenre.required(),
        language: movieRules.language.required(),
        censorship: movieRules.censorship.required(),
        description: movieRules.description.optional(),
        director: movieRules.director.required(),
        actors: movieRules.actors.required(),
        country: movieRules.country.required(),
        status: movieRules.status.optional(),
        poster: movieRules.poster.optional(),
        rating: movieRules.rating.optional(),
        totalReviews: movieRules.totalReviews.optional(),
        totalStarts: movieRules.totalStarts.optional()
    });

    try {
        // Allow allowUnknown: true to skip auto-generated or undefined fields if sent
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: true });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

//  Validator for PUT /api/admin/movies/:id 
const updateMovie = async (req, res, next) => {
    const correctCondition = Joi.object({
        movieName: movieRules.movieName.optional(),
        duration: movieRules.duration.optional(),
        releaseDate: movieRules.releaseDate.optional(),
        endDate: movieRules.endDate.optional(),
        movieGenre: movieRules.movieGenre.optional(),
        language: movieRules.language.optional(),
        censorship: movieRules.censorship.optional(),
        description: movieRules.description.optional(),
        director: movieRules.director.optional(),
        actors: movieRules.actors.optional(),
        country: movieRules.country.optional(),
        status: movieRules.status.optional(),
        poster: movieRules.poster.optional(),
        rating: movieRules.rating.optional(),
        totalReviews: movieRules.totalReviews.optional(),
        totalStarts: movieRules.totalStarts.optional()
    });

    try {
        await correctCondition.validateAsync(req.body, { abortEarly: false, allowUnknown: true });
        next();
    } catch (error) {
        const errorMessage = error.details ? error.details.map(err => err.message).join(', ') : error.message;
        next(new ApiError(StatusCodes.UNPROCESSABLE_ENTITY, errorMessage));
    }
};

export const AdminMovieValidation = {
    createMovie,
    updateMovie
};