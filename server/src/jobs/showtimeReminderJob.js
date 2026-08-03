import cron from "node-cron";
import { Booking } from "../models/bookingModel.js";
import { Account } from "../models/accountModel.js";
import { NotificationService } from "../services/notificationService.js";
import { getShowtimeTimeRange } from "../utils/formatters.js";

const runShowtimeReminder = async () => {
    try {
        const now = new Date();
        const fiveMinutesAgo = new Date(now.getTime() - 5 * 60 * 1000);
        const thirtyMinutesFromNow = new Date(now.getTime() + 30 * 60 * 1000);

        // Find bookings that need reminders
        const bookings = await Booking.find({
            paymentStatus: { $in: ['paid', 'successful'] },
            reminded30M: { $ne: true }
        }).populate({
            path: 'showtimeId',
            populate: [
                { path: 'movieId', select: 'movieName' },
                { path: 'roomId', select: 'roomName' }
            ]
        }).populate('memberId');

        for (const booking of bookings) {
            // Check if booking has valid showtime and member
            if (!booking.showtimeId || !booking.memberId) {
                // If it's malformed, mark it as reminded so we don't process it again
                booking.reminded30M = true;
                await booking.save();
                continue;
            }

            const startTime = booking.showtimeId.startTime;
            if (!startTime) {
                booking.reminded30M = true;
                await booking.save();
                continue;
            }

            // Check if the showtime is within [now - 5m, now + 30m]
            if (startTime >= fiveMinutesAgo && startTime <= thirtyMinutesFromNow) {
                try {
                    const account = await Account.findById(booking.memberId.accountId).select('username fcmToken fcmTokens');
                    if (account) {
                        const tokens = account.fcmTokens && account.fcmTokens.length > 0 
                            ? account.fcmTokens 
                            : (account.fcmToken ? [account.fcmToken] : [])
                        
                        if (tokens.length > 0) {
                            const movieName = booking.showtimeId.movieId?.movieName || 'Movie';
                            const roomName = booking.showtimeId.roomId?.roomName || 'Theater';
                            const showTimeStr = getShowtimeTimeRange(booking.showtimeId);

                            const title = 'Upcoming Showtime Reminder!';
                            const body = `Your movie "${movieName}" is starting soon at ${showTimeStr} in Room ${roomName}. Please prepare to join!`;

                            await NotificationService.sendToDevice(tokens, title, body, {
                                type: 'booking',
                                referenceId: booking._id.toString()
                            });
                            console.log(`[Reminder] Sent showtime reminder to user ${account.username} for booking ${booking.bookingCode}`);
                        }
                    }
                } catch (err) {
                    console.error(`[Reminder] Failed to send reminder for booking ${booking.bookingCode}:`, err.message);
                }
                
                // Mark as reminded regardless of FCM delivery success to avoid duplicate alerts
                booking.reminded30M = true;
                await booking.save();
            } else if (startTime < fiveMinutesAgo) {
                // Showtime has passed the window without being processed, just mark it as reminded
                booking.reminded30M = true;
                await booking.save();
            }
        }
    } catch (error) {
        console.error('[Reminder] Error running showtime reminder job:', error.message);
    }
};

const startShowtimeReminderJob = () => {
    // Run every minute
    cron.schedule("* * * * *", runShowtimeReminder);
    console.log('⏰ Showtime reminder cron job scheduled (every minute).');
};

export const ShowtimeReminderJob = {
    startShowtimeReminderJob,
    runShowtimeReminder
};
