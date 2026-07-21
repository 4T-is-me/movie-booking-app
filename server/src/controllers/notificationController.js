import { Notification } from '../models/notificationModel.js';
import { StatusCodes } from 'http-status-codes';
import { NotificationService } from '../services/notificationService.js';

export const NotificationController = {
  // 1. Lấy danh sách thông báo của User đang đăng nhập (Member / Staff / Admin)
  async getMyNotifications(req, res, next) {
    try {
      // Lấy ID trực tiếp từ User đã authen (tương ứng với _id của Account)
      const accountId =  req.user.accountId;

      // Nếu muốn dùng Service chung (hỗ trợ phân trang + unread count)
      const result = await NotificationService.getStaffNotifications(accountId, req.query);

      return res.status(StatusCodes.OK).json({
        success: true,
        message: 'Lấy danh sách thông báo thành công',
        data: result.notifications,
        pagination: result.pagination,
        unreadCount: result.unreadCount
      });
    } catch (error) {
      next(error);
    }
  },

  // 2. Lấy riêng số lượng chưa đọc (Cho icon Chuông trên Header)
  async getUnreadCount(req, res, next) {
    try {
      const accountId =  req.user.accountId;

      const count = await Notification.countDocuments({
        recipientId: accountId,
        isRead: false
      });

      return res.status(StatusCodes.OK).json({ success: true, count });
    } catch (error) {
      next(error);
    }
  },

  // 3. Đánh dấu 1 thông báo là đã đọc
  async markAsRead(req, res, next) {
    try {
      const accountId = req.user.accountId;
      const { id } = req.params; // ID của thông báo

      // Tìm thông báo theo cả _id và recipientId để đảm bảo đúng chủ sở hữu
      const updated = await NotificationService.markAsRead(id, accountId);

      if (!updated) {
        return res.status(StatusCodes.NOT_FOUND).json({
          success: false,
          message: 'Không tìm thấy thông báo hoặc bạn không có quyền'
        });
      }

      return res.status(StatusCodes.OK).json({
        success: true,
        message: 'Đã đánh dấu là đã đọc',
        data: updated
      });
    } catch (error) {
      next(error);
    }
  },

  // 4. Đánh dấu tất cả thông báo là đã đọc
  async markAllAsRead(req, res, next) {
    try {
      const accountId = req.user.accountId;
      await NotificationService.markAllAsRead(accountId);

      return res.status(StatusCodes.OK).json({
        success: true,
        message: 'Đã đánh dấu tất cả thông báo là đã đọc'
      });
    } catch (error) {
      next(error);
    }
  }
};