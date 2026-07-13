import mongoose from 'mongoose';
import { Review } from '../models/reviewModel.js';
import { Movie } from '../models/movieModel.js';
import { Member } from '../models/memberModel.js';
import { Booking } from '../models/bookingModel.js';
import { Showtime } from '../models/showTimeModel.js';
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

    // 2. Verify user has booking history for this movie (paymentStatus = paid/completed/successful)
    const member = await Member.findOne({ accountId });
    if (!member) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Member profile not found. Review submission denied.');
    }

    // Get all showtime IDs for this movie
    const showtimes = await Showtime.find({ movieId, isDeleted: false }).select('_id');
    const showtimeIds = showtimes.map(st => st._id);

    // Find any successful booking count for these showtimes
    const bookingCount = await Booking.countDocuments({
        memberId: member._id,
        showtimeId: { $in: showtimeIds },
        paymentStatus: { $in: ['paid', 'completed', 'successful'] }
    });

    if (bookingCount === 0) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'You must have purchased a successful ticket for this movie to submit a review.');
    }

    const reviewCount = await Review.countDocuments({
        accountId,
        movieId
    });

    if (reviewCount >= bookingCount) {
        throw new ApiError(StatusCodes.FORBIDDEN, 'Review count limit reached. You can only submit one review per purchase/booking.');
    }

    // 3. Validate input constraints
    const numericRating = Number(rating);
    if (isNaN(numericRating) || numericRating < 1 || numericRating > 5) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Rating must be a number between 1 and 5.');
    }

    const trimmedComment = String(comment).trim();
    if (trimmedComment.length < 5 || trimmedComment.length > 1000) {
        throw new ApiError(StatusCodes.BAD_REQUEST, 'Comment must be between 5 and 1000 characters long.');
    }

    // 4. Create and save the review
    const review = new Review({
        accountId,
        movieId,
        rating: numericRating,
        comment: trimmedComment
    });

    await review.save();

    // 5. Recalculate average rating, total reviews, and update Movie document
    const stats = await Review.aggregate([
        { $match: { movieId: new mongoose.Types.ObjectId(movieId) } },
        {
            $group: {
                _id: '$movieId',
                averageRating: { $avg: '$rating' },
                totalCount: { $sum: 1 },
                sumStars: { $sum: '$rating' }
            }
        }
    ]);

    if (stats.length > 0) {
        const avg = Math.round(stats[0].averageRating * 10) / 10;
        await Movie.findByIdAndUpdate(movieId, {
            rating: avg,
            totalReviews: stats[0].totalCount,
            totalStarts: stats[0].sumStars
        });
    } else {
        await Movie.findByIdAndUpdate(movieId, {
            rating: 0,
            totalReviews: 0,
            totalStarts: 0
        });
    }

    return review;
};

const getMovieReviews = async (movieId, page = 1, limit = 10) => {
    // 1. Verify movie exists
    const movie = await Movie.findById(movieId);
    if (!movie) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Movie not found.');
    }

    const skip = (page - 1) * limit;
    const totalReviews = await Review.countDocuments({ movieId });

    const reviews = await Review.find({ movieId })
        .populate({
            path: 'accountId',
            select: 'username',
            populate: {
                path: 'memberInfo',
                select: 'fullName image'
            }
        })
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .lean();

    return {
        reviews,
        pagination: {
            totalReviews,
            page,
            limit,
            totalPages: Math.ceil(totalReviews / limit)
        }
    };
};

export const reviewService = {
    createReview,
    getMovieReviews
};
