import mongoose from 'mongoose';

const newsSchema = new mongoose.Schema({
    title: { type: String, required: true },
    content: { type: String, required: true },
    image: { type: String },
    publishedAt: { type: Date, default: Date.now },
    movieId: { type: mongoose.Schema.Types.ObjectId, ref: 'Movie', default: null }, // Optional Movie tag
    authorId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true },
    likes: [{ type: mongoose.Schema.Types.ObjectId, ref: 'Account' }]
}, { timestamps: true, versionKey: false, collection: 'news' });

// Index for query performance on chronological loading
newsSchema.index({ publishedAt: -1 });

export const News = mongoose.model('News', newsSchema);
