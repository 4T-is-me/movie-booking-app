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
