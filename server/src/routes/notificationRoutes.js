import express from 'express';
import { NotificationController } from '../controllers/notificationController.js';
import { authMiddleware } from '../middlewares/authMiddleware.js';

const router = express.Router();

// Bắt buộc tất cả các route thông báo phải đăng nhập (yêu cầu Token)
router.use(authMiddleware.isAuthorized);

// 1. Lấy danh sách thông báo của tài khoản đang đăng nhập (Member, Staff, Admin...)
// GET /api/notifications
router.get('/', NotificationController.getMyNotifications);

// 2. Lấy số lượng thông báo chưa đọc (hiển thị chấm đỏ)
// GET /api/notifications/unread-count
router.get('/unread-count', NotificationController.getUnreadCount);

// 3. Đánh dấu TẤT CẢ thông báo là đã đọc (Đặt trên route /:id để tránh bị nhầm 'read-all' là 1 id)
// PATCH /api/notifications/read-all
router.patch('/read-all', NotificationController.markAllAsRead);

// 4. Đánh dấu 1 thông báo cụ thể là đã đọc
// PATCH /api/notifications/:id/read
router.patch('/:id/read', NotificationController.markAsRead);

export default router;