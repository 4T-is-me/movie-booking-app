import mongoose from 'mongoose';

const movieSchema = new mongoose.Schema({
    movieName: { type: String, required: true, trim: true },
    duration: { type: Number, required: true }, // Duration (minutes)
    releaseDate: { type: Date, required: true }, // Release date
    endDate: { type: Date },                      // End date
    movieGenre: { type: [String], required: true }, // Array of movie genres
    language: { type: String, required: true },
    censorship: { type: String, required: true }, // Censorship rating (e.g. 13+)
    description: { type: String }, // Movie description
    director: { type: [String], required: true }, // Array of director names
    actors: { type: [String], required: true }, // Array of actor names
    country: { type: String, required: true },
    status: { type: String, default: 'coming_soon' }, // Movie status (ended, showing,...)
    poster: { type: String }, // Poster image link
    rating: { type: Number, default: 0 },
    totalReviews: { type: Number, default: 0 },
    totalStarts: { type: Number, default: 0 },
    isDeleted: { type: Boolean, default: false },
    deletedAt: { type: Date, default: null }
}, {
    versionKey: false,
    collection: 'movies', // Match collection name
    timestamps: true // Automatically manage createdAt and updatedAt as ISODate
});

// Optimized index for filtering movies by release date range (now_showing / coming_soon)
movieSchema.index({ releaseDate: 1, endDate: 1 });
// Index to filter active movies and sort by release date
movieSchema.index({isDeleted: 1, releaseDate: 1}); 
export const Movie = mongoose.model('Movie', movieSchema);
