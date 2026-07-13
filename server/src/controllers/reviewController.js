import mongoose from 'mongoose';
import { reviewService } from '../services/reviewService.js';
import { StatusCodes } from 'http-status-codes';

/**
 * POST /api/movies/:id/reviews
 * Creates a review for a movie.
 */
export const handleCreateReview = async (req, res, next) => {
    try {
        const movieId = req.params.id;
        const accountId = req.user?.accountId;
        const { rating, comment } = req.body;

        // Validate movie ID
        if (!mongoose.isValidObjectId(movieId)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid movie ID format.'
            });
        }

        // Validate account ID
        if (!accountId) {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: 'User authentication credentials not found.'
            });
        }

        // Validate request body
        if (rating === undefined || comment === undefined) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Missing required fields: rating, comment.'
            });
        }

        const review = await reviewService.createReview(accountId, movieId, rating, comment);

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: review,
            message: 'Review submitted successfully.'
        });
    } catch (error) {
        next(error);
    }
};

/**
 * GET /api/movies/:id/reviews
 * Fetches review list for a movie with pagination and sorting.
 */
export const handleGetMovieReviews = async (req, res, next) => {
    try {
        const movieId = req.params.id;
        const page = Number(req.query.page) || 1;
        const limit = Number(req.query.limit) || 10;

        // Validate movie ID
        if (!mongoose.isValidObjectId(movieId)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid movie ID format.'
            });
        }

        const result = await reviewService.getMovieReviews(movieId, page, limit);

        return res.status(StatusCodes.OK).json({
            success: true,
            data: result.reviews,
            pagination: result.pagination
        });
    } catch (error) {
        next(error);
    }
};

/**
 * POST /api/reviews/:reviewId/like
 * Toggles a user's like on a review.
 */
export const handleToggleLike = async (req, res, next) => {
    try {
        const reviewId = req.params.reviewId;
        const accountId = req.user?.accountId;

        if (!mongoose.isValidObjectId(reviewId)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid review ID format.'
            });
        }

        if (!accountId) {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: 'User authentication credentials not found.'
            });
        }

        const result = await reviewService.toggleLikeReview(reviewId, accountId);

        return res.status(StatusCodes.OK).json({
            success: true,
            data: result,
            message: result.isLiked ? 'Review liked successfully.' : 'Review unliked successfully.'
        });
    } catch (error) {
        next(error);
    }
};

/**
 * POST /api/reviews/:reviewId/replies
 * Appends a reply to a review.
 */
export const handleCreateReply = async (req, res, next) => {
    try {
        const reviewId = req.params.reviewId;
        const accountId = req.user?.accountId;
        const { comment } = req.body;

        if (!mongoose.isValidObjectId(reviewId)) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Invalid review ID format.'
            });
        }

        if (!accountId) {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: 'User authentication credentials not found.'
            });
        }

        if (comment === undefined) {
            return res.status(StatusCodes.BAD_REQUEST).json({
                success: false,
                message: 'Missing required field: comment.'
            });
        }

        const reply = await reviewService.createReply(reviewId, accountId, comment);

        return res.status(StatusCodes.CREATED).json({
            success: true,
            data: reply,
            message: 'Reply posted successfully.'
        });
    } catch (error) {
        next(error);
    }
};
