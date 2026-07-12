import { messaging } from '../config/firebaseAdmin.js'
import { Account } from '../models/accountModel.js'
import { Showtime } from '../models/showTimeModel.js'
import { Movie } from '../models/movieModel.js'
import { CinemaRoom } from '../models/cinemaRoomModel.js'

/**
 * Sends a push notification to a specific user.
 *
 * @param {string} token - The recipient's FCM registration token.
 * @param {string} title - Notification title.
 * @param {string} body - Notification body text.
 * @param {Object} [data] - Optional metadata payload (e.g., { type: 'booking', referenceId: '...' }).
 */
const sendToDevice = async (token, title, body, data = {}) => {
    if (!messaging) {
        console.warn('Firebase Admin SDK is not initialized. Skipping notification.')
        return
    }
    if (!token) return

    try {
        const message = {
            token: token,
            notification: {
                title: title,
                body: body
            },
            data: data
        }
        const response = await messaging.send(message)
        console.log(`Successfully sent message to device: ${response}`)
    } catch (error) {
        console.error(`Error sending message to device: ${error.message}`)
    }
}

/**
 * Sends a push notification to a topic (e.g., 'promotions').
 *
 * @param {string} topic - The Firebase topic name.
 * @param {string} title - Notification title.
 * @param {string} body - Notification body text.
 * @param {Object} [data] - Optional metadata payload.
 */
const sendToTopic = async (topic, title, body, data = {}) => {
    if (!messaging) {
        console.warn('Firebase Admin SDK is not initialized. Skipping topic notification.')
        return
    }

    try {
        const message = {
            topic: topic,
            notification: {
                title: title,
                body: body
            },
            data: data
        }
        const response = await messaging.send(message)
        console.log(`Successfully sent message to topic [${topic}]: ${response}`)
    } catch (error) {
        console.error(`Error sending message to topic [${topic}]: ${error.message}`)
    }
}

/**
 * Subscribes a token to the promotions topic.
 * Safe to call multiple times for the same token.
 *
 * @param {string} token - The FCM registration token.
 */
const subscribeToPromotions = async (token) => {
    if (!messaging || !token) return
    try {
        await messaging.subscribeToTopic([token], 'promotions')
        console.log(`Subscribed token ${token.substring(0, 10)}... to 'promotions' topic`)
    } catch (error) {
        console.error(`Error subscribing token to promotions topic: ${error.message}`)
    }
}

/**
 * Compiles booking information and pushes a notification to the customer.
 *
 * @param {string} accountId - User's account ID.
 * @param {Object} booking - The created booking document.
 */
const sendBookingSuccessNotification = async (accountId, booking) => {
    try {
        const account = await Account.findById(accountId).select('fcmToken')
        if (!account || !account.fcmToken) {
            console.log(`No FCM token found for account ${accountId}. Skipping booking notification.`)
            return
        }

        // Fetch showtime details to get Movie and Room name
        const showtime = await Showtime.findById(booking.showtimeId)
            .populate({ path: 'movieId', select: 'movieName' })
            .populate({ path: 'roomId', select: 'roomName' })
            .lean()

        const movieName = showtime?.movieId?.movieName || 'Booked movie'
        const roomName = showtime?.roomId?.roomName || 'Theater room'
        const seats = booking.seatNames ? booking.seatNames.join(', ') : ''

        const title = 'Booking successful!'
        const body = `Congratulations! You have successfully booked tickets for the movie "${movieName}". Room: ${roomName}, Seats: ${seats}. Enjoy your movie!`

        await sendToDevice(account.fcmToken, title, body, {
            type: 'booking',
            referenceId: booking._id.toString()
        })
    } catch (error) {
        console.error('Error compiling booking notification:', error.message)
    }
}

export const notificationService = {
    sendToDevice,
    sendToTopic,
    subscribeToPromotions,
    sendBookingSuccessNotification
}
