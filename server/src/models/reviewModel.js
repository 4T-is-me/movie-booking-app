import mongoose from 'mongoose';

const replySchema = new mongoose.Schema({
    accountId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Account',
        required: true
    },
    comment: {
        type: String,
        required: true,
        minlength: 1,
        maxlength: 500
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
}, { _id: true });

const reviewSchema = new mongoose.Schema({
    accountId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Account',
        required: true
    },
    movieId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Movie',
        required: true
    },
    rating: {
        type: Number,
        required: true,
        min: 1,
        max: 5 // standard 5-star rating system
    },
    comment: {
        type: String,
        required: true,
        minlength: 5,
        maxlength: 1000
    },
    likes: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Account'
    }],
    replies: [replySchema],
    createdAt: {
        type: Date,
        default: Date.now
    }
}, {
    timestamps: true,
    versionKey: false,
    collection: 'reviews'
});

// Indices for faster querying of reviews by movie or account
reviewSchema.index({ movieId: 1, createdAt: -1 });
reviewSchema.index({ accountId: 1, createdAt: -1 });

export const Review = mongoose.model('Review', reviewSchema);
