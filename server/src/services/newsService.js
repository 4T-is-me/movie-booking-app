import { News } from '../models/newsModel.js';
import { Comment } from '../models/commentModel.js';
import { Member } from '../models/memberModel.js';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';

const handleGetNews = async () => {
    return await News.find().sort({ publishedAt: -1 }).populate('movieId', 'movieName poster').lean();
};

const handleGetNewsById = async (id) => {
    const item = await News.findById(id).populate('movieId').lean();
    if (!item) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'News article not found.');
    }
    return item;
};

const handleGetComments = async (newsId) => {
    // 1. Fetch comments sorted chronologically
    const comments = await Comment.find({ newsId })
        .populate({ path: 'accountId', select: 'username' })
        .sort({ createdAt: 1 })
        .lean();

    // 2. Fetch members in one query to populate commenter profile pics & names
    const accountIds = comments.map(c => c.accountId?._id || c.accountId);
    const members = await Member.find({ accountId: { $in: accountIds } }).lean();
    const memberMap = new Map(members.map(m => [m.accountId.toString(), m]));

    // 3. Map comments to include user's name & avatar
    const mapped = comments.map(comment => {
        const userId = comment.accountId?._id?.toString() || comment.accountId?.toString();
        const memberInfo = memberMap.get(userId);
        return {
            ...comment,
            commenterName: memberInfo?.fullName || comment.accountId?.username || 'User',
            commenterAvatar: memberInfo?.image || '',
            reactionsSummary: summarizeReactions(comment.reactions)
        };
    });

    // 4. Build hierarchical tree structure
    const commentMap = new Map(mapped.map(c => [c._id.toString(), { ...c, replies: [] }]));
    const rootComments = [];

    for (const comment of commentMap.values()) {
        if (comment.parentId) {
            const parent = commentMap.get(comment.parentId.toString());
            if (parent) {
                parent.replies.push(comment);
            } else {
                // If parent comment was deleted or missing, treat as root
                rootComments.push(comment);
            }
        } else {
            rootComments.push(comment);
        }
    }

    return rootComments;
};

const handleCreateComment = async ({ newsId, accountId, content, parentId = null }) => {
    // Verify news article exists
    const news = await News.findById(newsId);
    if (!news) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'News article not found.');
    }

    // Verify parent comment exists if this is a reply
    if (parentId) {
        const parentComment = await Comment.findById(parentId);
        if (!parentComment) {
            throw new ApiError(StatusCodes.NOT_FOUND, 'Parent comment not found.');
        }
    }

    const newComment = new Comment({
        newsId,
        accountId,
        content: content.trim(),
        parentId
    });

    await newComment.save();

    // Populate commenter details for immediate response
    const populated = await Comment.findById(newComment._id)
        .populate({ path: 'accountId', select: 'username' })
        .lean();
    
    const memberInfo = await Member.findOne({ accountId }).lean();

    return {
        ...populated,
        commenterName: memberInfo?.fullName || populated.accountId?.username || 'User',
        commenterAvatar: memberInfo?.image || '',
        reactionsSummary: { count: 0, userReacted: null }
    };
};

const handleToggleReaction = async ({ commentId, accountId, type }) => {
    const comment = await Comment.findById(commentId);
    if (!comment) {
        throw new ApiError(StatusCodes.NOT_FOUND, 'Comment not found.');
    }

    // Check if user already reacted
    const existingIndex = comment.reactions.findIndex(
        r => r.accountId.toString() === accountId.toString()
    );

    if (existingIndex > -1) {
        const existingReaction = comment.reactions[existingIndex];
        if (existingReaction.type === type) {
            // Remove reaction if clicking the same type again
            comment.reactions.splice(existingIndex, 1);
        } else {
            // Update type if choosing a different reaction
            comment.reactions[existingIndex].type = type;
        }
    } else {
        // Add new reaction
        comment.reactions.push({ accountId, type });
    }

    await comment.save();
    return summarizeReactions(comment.reactions, accountId);
};

// Helper function to count and summarize reactions
const summarizeReactions = (reactions = [], currentAccountId = null) => {
    const count = reactions.length;
    let userReacted = null;

    if (currentAccountId) {
        const currentReaction = reactions.find(
            r => r.accountId.toString() === currentAccountId.toString()
        );
        userReacted = currentReaction ? currentReaction.type : null;
    }

    // Tally reaction types (e.g. { like: 2, love: 1 })
    const breakdown = {};
    reactions.forEach(r => {
        breakdown[r.type] = (breakdown[r.type] || 0) + 1;
    });

    return {
        count,
        userReacted,
        breakdown
    };
};

export const newsService = {
    handleGetNews,
    handleGetNewsById,
    handleGetComments,
    handleCreateComment,
    handleToggleReaction
};
