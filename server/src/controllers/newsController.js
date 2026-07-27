import { newsService } from '../services/newsService.js';
import { StatusCodes } from 'http-status-codes';

const getNews = async (req, res, next) => {
    try {
        const currentAccountId = req.user?.accountId || null;
        const result = await newsService.handleGetNews(currentAccountId);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const getNewsById = async (req, res, next) => {
    try {
        const { id } = req.params;
        const currentAccountId = req.user?.accountId || null;
        const result = await newsService.handleGetNewsById(id, currentAccountId);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const getComments = async (req, res, next) => {
    try {
        const { id } = req.params; // news ID
        const result = await newsService.handleGetComments(id);
        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const createComment = async (req, res, next) => {
    try {
        const { id } = req.params; // news ID
        const { content, parentId } = req.body;
        const accountId = req.user.accountId;

        const result = await newsService.handleCreateComment({
            newsId: id,
            accountId,
            content,
            parentId
        });

        return res.status(StatusCodes.CREATED).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const toggleReaction = async (req, res, next) => {
    try {
        const { commentId } = req.params;
        const { type } = req.body;
        const accountId = req.user.accountId;

        const result = await newsService.handleToggleReaction({
            commentId,
            accountId,
            type
        });

        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

const toggleNewsLike = async (req, res, next) => {
    try {
        const { id } = req.params;
        const accountId = req.user.accountId;

        const result = await newsService.handleToggleNewsLike({
            newsId: id,
            accountId
        });

        return res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
        next(error);
    }
};

export const newsController = {
    getNews,
    getNewsById,
    toggleNewsLike,
    getComments,
    createComment,
    toggleReaction
};
