import express from 'express';
import { staffController } from '../controllers/staffController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const staffRouter = express.Router();

// POST /api/staff/tickets/check-in - Check in a ticket by bookingCode
staffRouter.post('/tickets/check-in', authMiddleware.isAuthorized, staffController.checkInTicket);

// GET /api/staffs/my-ratings - Get my ratings
staffRouter.get('/my-ratings', authMiddleware.isAuthorized, staffController.getMyRatings);

export const staffRoute = {
    staffRouter
};
