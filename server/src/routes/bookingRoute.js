import express from 'express'
import {bookingController} from '../controllers/bookingController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'

const bookingRouter = express.Router()

// POST /api/bookings/confirm --check seats valid + total price + check and deduct points
bookingRouter.post('/confirm', authMiddleware.isAuthorized, bookingController.handleConfirmBooking)
// GET /api/bookings/history --get history
bookingRouter.get('/history', authMiddleware.isAuthorized, bookingController.handleGetHistory)
// GET /api/bookings/score-history --get score history
bookingRouter.get('/score-history', authMiddleware.isAuthorized, bookingController.handleGetScoreHistory)
export const bookingRoute = {
    bookingRouter
}