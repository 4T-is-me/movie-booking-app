import mongoose from 'mongoose';

const commentSchema = new mongoose.Schema({
    newsId: { type: mongoose.Schema.Types.ObjectId, ref: 'News', required: true },
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true },
    content: { type: String, required: true },
    parentId: { type: mongoose.Schema.Types.ObjectId, ref: 'Comment', default: null }, // Null for top-level comments
    reactions: [{
        accountId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true },
        type: { type: String, enum: ['like', 'love', 'haha', 'wow', 'sad', 'angry'], required: true }
    }]
}, { timestamps: true, versionKey: false, collection: 'comments' });

// Add compound index for fast chronological loading per news article
commentSchema.index({ newsId: 1, createdAt: 1 });
commentSchema.index({ parentId: 1 });

export const Comment = mongoose.model('Comment', commentSchema);
