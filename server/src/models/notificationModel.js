import mongoose from 'mongoose';

const notificationSchema = new mongoose.Schema({
    recipientId: { type: mongoose.Schema.Types.ObjectId, ref: 'Member', required: true },
    title: { type: String, required: true }, // Tiêu đề thông báo
    message: { type: String, required: true }, // Nội dung hiển thị
    isRead: { type: Boolean, default: false }, // Đánh dấu đã đọc
    type: { type: String, required: true }, // e.g. "ACTION_REQUIRED", "INFO", "PROMOTION"
    metadata: { type: mongoose.Schema.Types.Mixed }, // { bookingId, newRoomId, newShowtimeId, etc. }
    expireAt: { type: Date, required: true }
}, { timestamps: true, versionKey: false, collection: 'notifications' });

// Add TTL Index to automatically delete documents after expireAt
notificationSchema.index({ expireAt: 1 }, { expireAfterSeconds: 0 });

export const Notification = mongoose.model('Notification', notificationSchema);
