import { Review } from '../models/reviewModel.js';
import { Movie } from '../models/movieModel.js';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';

/**
 * Creates a new movie review.
 * 
 * @param {string} accountId - User's account ID.
 * @param {string} movieId - Movie's ID.
 * @param {number} rating - Rating score (1 to 5).
 * @param {string} comment - Review text.
 */
const createReview = async (accountId, movieId, rating, comment) => {
    // 1. Verify movie exists
    const movie = await Movie.findById(movieId);
    if (!movie) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found.');
    }

    // 2. Validate input constraints
    const numericRating = Number(rating);
    if (isNaN(numericRating) || numericRating < 1 || numericRating > 5) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Rating must be a number between 1 and 5.');
    }

    const trimmedComment = String(comment).trim();
    if (trimmedComment.length < 5 || trimmedComment.length > 1000) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Comment must be between 5 and 1000 characters long.');
    }

    // 3. Create and save the review
    const review = new Review({
        accountId,
        movieId,
        rating: numericRating,
        comment: trimmedComment,
        likes: [],
        replies: []
    });

    await review.save();
    return review;
};

export const reviewService = {
    createReview
};
