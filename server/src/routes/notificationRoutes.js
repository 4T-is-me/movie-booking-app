import express from 'express';
import { NotificationController } from '../controllers/notificationController.js';

const router = express.Router();

// Lấy danh sách thông báo của 1 member
router.get('/:memberId', NotificationController.getUserNotifications);

// Lấy số lượng thông báo chưa đọc
router.get('/:memberId/unread-count', NotificationController.getUnreadCount);

// Đánh dấu thông báo đã đọc
router.patch('/:id/read', NotificationController.markAsRead);

export default router;
