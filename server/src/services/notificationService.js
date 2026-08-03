import { messaging } from '../config/firebaseAdmin.js'
import { Account } from '../models/accountModel.js'
import { Showtime } from '../models/showTimeModel.js'
import { Notification } from '../models/notificationModel.js'

/**
 * Sends a push notification to a specific user.
 *
 * @param {string} token - The recipient's FCM registration token.
 * @param {string} title - Notification title.
 * @param {string} body - Notification body text.
 * @param {Object} [data] - Optional metadata payload (e.g., { type: 'booking', referenceId: '...' }).
 */
const sendToDevice = async (tokens, title, body, data = {}) => {
    if (!messaging) {
        console.warn('Firebase Admin SDK is not initialized. Skipping notification.')
        return
    }
    if (!tokens) return

    const tokenList = Array.isArray(tokens) ? tokens : [tokens]
    const validTokens = tokenList.filter(t => t && typeof t === 'string' && t.trim() !== '')
    if (validTokens.length === 0) return

    for (const token of validTokens) {
        try {
            // Ensure all values in the data payload are strings to prevent Firebase validation errors
            const stringData = {}
            for (const [key, val] of Object.entries(data)) {
                if (val !== undefined && val !== null) {
                    stringData[key] = String(val)
                }
            }

            const message = {
                token: token,
                notification: {
                    title: title,
                    body: body
                },
                data: {
                    title: String(title),
                    body: String(body),
                    ...stringData
                }
            }
            const response = await messaging.send(message)
            console.log(`Successfully sent message to device: ${response}`)
        } catch (error) {
            console.error(`Error sending message to device (${token.substring(0, 10)}...): ${error.message}`)
            // Clean up invalid or unregistered tokens from the database to keep it clean
            if (
                error.code === 'messaging/registration-token-not-registered' ||
                error.code === 'messaging/invalid-registration' ||
                error.message.includes('not registered') ||
                error.message.includes('invalid-registration')
            ) {
                console.log(`Removing unregistered/invalid token from database: ${token.substring(0, 10)}...`)
                await Account.updateMany(
                    { $or: [{ fcmToken: token }, { fcmTokens: token }] },
                    { 
                        $set: { fcmToken: null },
                        $pull: { fcmTokens: token }
                    }
                )
            }
        }
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
        // Ensure all values in the data payload are strings to prevent Firebase validation errors
        const stringData = {}
        for (const [key, val] of Object.entries(data)) {
            if (val !== undefined && val !== null) {
                stringData[key] = String(val)
            }
        }

        const message = {
            topic: topic,
            notification: {
                title: title,
                body: body
            },
            data: {
                title: String(title),
                body: String(body),
                ...stringData
            }
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
        const account = await Account.findById(accountId).select('fcmToken fcmTokens')
        const tokens = account?.fcmTokens && account.fcmTokens.length > 0 
            ? account.fcmTokens 
            : (account?.fcmToken ? [account.fcmToken] : [])

        if (tokens.length === 0) {
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

        await sendToDevice(tokens, title, body, {
            type: 'booking',
            referenceId: booking._id.toString()
        })
    } catch (error) {
        console.error('Error compiling booking notification:', error.message)
    }
}

/**
 * Creates notification in DB and pushes to Android device
 */
const createNotification = async ({ accountId, title, message, body, type = 'general', metadata = {}, expireDays = 30 }) => {
    try {
        const account = await Account.findById(accountId);
        if (!account) {
            console.error(`[Notification Error] Account not found with ID: ${accountId}`);
            return null;
        }

        const notificationContent = message || body || 'You have a new notification';

        const newNotification = await Notification.create({
            recipientId: account._id,
            title: title || 'Notification',
            message: notificationContent,
            type,
            metadata,
            expireAt: new Date(Date.now() + expireDays * 24 * 60 * 60 * 1000)
        });

        const tokens = account.fcmTokens && account.fcmTokens.length > 0 
            ? account.fcmTokens 
            : (account.fcmToken ? [account.fcmToken] : [])

        if (tokens.length > 0) {
            try {
                // Map metadata to string values and normalize referenceId
                const stringifiedMetadata = {
                    notificationId: String(newNotification._id),
                    type: String(type),
                    referenceId: String(metadata.leaveRequestId || metadata.id || metadata.referenceId || '')
                };

                Object.keys(metadata).forEach(key => {
                    stringifiedMetadata[key] = String(metadata[key]);
                });

                await sendToDevice(
                    tokens,
                    title || 'Notification',
                    notificationContent,
                    stringifiedMetadata
                );
            } catch (fcmError) {
                console.error(`[FCM Push Error] Failed to push to account ${accountId}:`, fcmError);
            }
        }

        return newNotification;
    } catch (error) {
        console.error("[Notification Service Error]:", error);
        return null; 
    }
}

const getStaffNotifications = async (staffId, query) => {
    const page = parseInt(query.page, 10) || 1;
    const limit = parseInt(query.limit, 10) || 10;
    const skip = (page - 1) * limit;

    const filter = { recipientId: staffId };

    // Filter by read status (e.g. if client passes ?isRead=false)
    if (query.isRead !== undefined) {
        filter.isRead = query.isRead === 'true';
    }

    // Filter by notification type (e.g. if client passes ?type=ACTION_REQUIRED)
    if (query.type) {
        filter.type = query.type;
    }

    const [notifications, total, unreadCount] = await Promise.all([
        Notification.find(filter)
            .sort({ createdAt: -1 }) // Newest first
            .skip(skip)
            .limit(limit)
            .lean(),
        Notification.countDocuments(filter),
        Notification.countDocuments({ recipientId: staffId, isRead: false })
    ]);

    return {
        notifications,
        pagination: {
            total,
            page,
            limit,
            totalPages: Math.ceil(total / limit)
        },
        unreadCount
    };
};

const markAsRead = async (notificationId, staffId) => {
    return await Notification.findOneAndUpdate(
        { _id: notificationId, recipientId: staffId },
        { isRead: true },
        { returnDocument: 'after' }
    );
};


const markAllAsRead = async (staffId) => {
    return await Notification.updateMany(
        { recipientId: staffId, isRead: false },
        { isRead: true }
    );
};
export const NotificationService = {
    sendToDevice,
    sendToTopic,
    subscribeToPromotions,
    sendBookingSuccessNotification,
    createNotification,
    getStaffNotifications,
    markAsRead,
    markAllAsRead
}
