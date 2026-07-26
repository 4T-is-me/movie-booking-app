import express from 'express'
import {bookingController} from '../controllers/bookingController.js'
import { authMiddleware } from '../middlewares/authMiddleware.js'

const bookingRouter = express.Router()

// POST /api/booking/checkout --check seats valid + total price + check and deduct points
bookingRouter.post('/checkout', authMiddleware.isAuthorized, bookingController.handleConfirmBooking)
// GET /api/booking/history --get history
bookingRouter.get('/history', authMiddleware.isAuthorized, bookingController.handleGetHistory)
// GET /api/booking/score-history --get score history
bookingRouter.get('/score-history', authMiddleware.isAuthorized, bookingController.handleGetScoreHistory)
// GET /api/booking/my-tickets --get active tickets
bookingRouter.get('/my-tickets', authMiddleware.isAuthorized, bookingController.handleGetMyTickets)
// POST /api/booking/:id - get booking detail
bookingRouter.get('/:id', authMiddleware.isAuthorized, bookingController.handleGetBookingDetail)
// POST /api/booking/:id/rate-staff - rate staff
bookingRouter.post('/:id/rate-staff', authMiddleware.isAuthorized, bookingController.handleRateStaff)
export const bookingRoute = {
    bookingRouter
}